/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.actions.filetree

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.blankj.utilcode.util.FileIOUtils
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.requireFile
import com.itsaky.androidide.adapters.viewholders.FileTreeViewHolder
import com.itsaky.androidide.databinding.LayoutCreateFileJavaBinding
import com.itsaky.androidide.eventbus.events.file.FileCreationEvent
import com.itsaky.androidide.preferences.databinding.LayoutDialogDoubleTextInputBinding
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.utils.DialogUtils
import com.itsaky.androidide.utils.Environment
import com.itsaky.androidide.utils.ProjectWriter
import com.itsaky.androidide.utils.SingleTextWatcher
import com.itsaky.androidide.utils.flashError
import com.itsaky.androidide.utils.flashSuccess
import com.unnamed.b.atv.model.TreeNode
import jdkx.lang.model.SourceVersion
import org.greenrobot.eventbus.EventBus
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.Objects
import java.util.regex.Pattern
import com.itsaky.androidide.actions.ActionsRegistry
import com.itsaky.androidide.actions.internal.DefaultActionsRegistry
import com.itsaky.androidide.actions.ActionItem.Location.EDITOR_TOOLBAR
import com.itsaky.androidide.actions.ActionItem

/**
 * File tree action to create a new module.
 *
 * @author Miyaz Kaori
 */
class NewModuleAction(context: Context, override val order: Int) :
  BaseDirNodeAction(
    context = context,
    labelRes = R.string.new_module,
    iconRes = R.drawable.ic_new_module
  ) {

  override val id: String = "ide.editor.fileTree.newModule"

  override suspend fun execAction(data: ActionData) {
    val context = data.requireActivity()
    val currentDir = data.requireFile()
    val lastHeld = data.getTreeNode()
    val binding = LayoutDialogDoubleTextInputBinding.inflate(LayoutInflater.from(context))
    val builder = DialogUtils.newMaterialDialogBuilder(context)
    binding.moduleName.editText!!.setHint(R.string.module_name)
    binding.modulePkgName.editText!!.setHint(R.string.module_pkg_name)
    builder.setTitle(R.string.new_module)
    builder.setMessage(R.string.msg_can_contain_slashes)
    builder.setView(binding.root)
    builder.setCancelable(false)
    
    builder.setPositiveButton(R.string.text_create) { dialogInterface, _ ->
      dialogInterface.dismiss()
      
      val projectDir = IProjectManager.getInstance().projectDirPath
      Objects.requireNonNull(projectDir)
      
      val name: String = binding.moduleName.editText!!.text.toString().trim()
      if (name.length !in 1..40 || name.startsWith("/")) {
        flashError(R.string.msg_invalid_name)
        return@setPositiveButton
      }
      
      val pkgName: String = binding.modulePkgName.editText!!.text.toString().trim()
      if (pkgName.length !in 1..40 || pkgName.contains("/")) {
        flashError(R.string.msg_invalid_name)
        return@setPositiveButton
      }

      val newDir = File(currentDir, name)
      if (newDir.exists()) {
        flashError(R.string.msg_folder_exists)
        return@setPositiveButton
      }

      if (!newDir.mkdirs()) {
        flashError(R.string.msg_folder_creation_failed)
        return@setPositiveButton
      }
      
      // 开始创建模块
      if (!createModule(context, newDir, name, pkgName)) {
        flashError(R.string.msg_module_creation_failed)
        return@setPositiveButton
      }
      
      // 开始配置模块
      if(!configureModule(projectDir, newDir)) {
        flashError(R.string.msg_module_configure_failed)
        return@setPositiveButton
      }

      // flashSuccess(R.string.msg_module_created)
      if (lastHeld != null) {
        val node = TreeNode(newDir)
        node.viewHolder = FileTreeViewHolder(context)
        lastHeld.addChild(node)
        requestExpandNode(lastHeld)
      } else {
        requestFileListing()
      }
      
      // Sync the module
      
      val registry = ActionsRegistry.getInstance() as DefaultActionsRegistry
      val action = registry.findAction(EDITOR_TOOLBAR, "ide.editor.syncProject")
      
      if(action != null) {
        registry.executeAction(action, data)
      } else {
        flashError(R.string.msg_module_sync_failed)
      }
      
      
      /*
      data.requireActivity().lifecycleScope.launch {
        data.requireActivity().saveAll(requestSync = true)
      }
      */
    }
    builder.setNegativeButton(android.R.string.cancel, null)
    builder.create().show()
  }
  
  fun configureModule(projectDir: String, moduleDir: File) : Boolean {
    val projectPath = projectDir
    val modulePath = moduleDir.getAbsolutePath()
    
    var moduleName = modulePath.replace(projectPath + "/", "").replace("/", ":")
    
    // Update the settings.gradle
    var settingsFile = File(projectDir + "/settings.gradle")
    if (!settingsFile.exists()) {
      settingsFile = File(projectDir + "/settings.gradle.kts")
      if (!settingsFile.exists()) {
        flashError(R.string.msg_not_found_settings_file)
        return false
      }
    }
    
    try {
      settingsFile.appendText("\ninclude(\":$moduleName\")")
      return true
    } catch (e: IOException) {
      e.printStackTrace()
      flashError(R.string.msg_settings_file_update_failed)
    }
    
    return false
  }
  
  private fun createModule(context: Context, currentDir: File, name: String, pkgName: String) : Boolean {
    // 创建java目录
    if (!createDir(File(currentDir, "src/main/java/${pkgName.replace('.', '/')}"))) {
      return false
    }
    
    // 创建res目录
    if (!createDir(File(currentDir, "src/main/res"))) {
      return false
    }
    
    var content = """<?xml version="1.0" encoding="utf-8"?>

<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    
</manifest>
"""
    
    // 创建AndroidManifest.xml
    if (!createFile(context, File(currentDir, "src/main/AndroidManifest.xml"), content)) {
      return false
    }
    
    // 创建.gitignore
    content = """/build
"""
    if (!createFile(context, File(currentDir, ".gitignore"), content)) {
      return false
    }

    // 创建proguard-rules.pro
    content = """# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
"""
    if (!createFile(context, File(currentDir, "proguard-rules.pro"), content)) {
      return false
    }
    
    // 创建build.gradle
    content = """
plugins {
    id 'com.android.library'
    
}

android {
    namespace '$pkgName'
    
    compileSdk 36

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    // implementation 'androidx.core:core:1.17.0'
    // implementation 'androidx.annotation:annotation:1.9.1'
}
"""
    if (!createFile(context, File(currentDir, "build.gradle"), content)) {
      return false
    }
    
    return true
  }
  
  private fun createFile(context: Context, file: File, content: String) : Boolean {
    try {
      file.writeText(content)
      // notifyFileCreated(file, context)
      return true
    } catch (e: IOException) {
      e.printStackTrace()
      flashError(R.string.msg_file_creation_failed)
    }
    
    return false
  }
  
  private fun createDir(dir: File): Boolean {
    if (!dir.mkdirs()) {
      flashError(R.string.msg_folder_creation_failed)
      return false
    }
    
    return true
  }
  
  private fun notifyFileCreated(file: File, context: Context) {
    EventBus.getDefault().post(FileCreationEvent(file).putData(context))
  }
}
