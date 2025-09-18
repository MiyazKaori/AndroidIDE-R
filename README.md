<p align="center">
  <img src="./images/icon.png" alt="AndroidIDE" width="80" height="80"/>
</p>

<h2 align="center"><b>AndroidIDE</b></h2>
<p align="center">
  An IDE to develop real, Gradle-based Android applications on Android devices.
<p><br>

<p align="center">
<!-- Latest release -->
<img src="https://img.shields.io/github/v/release/AndroidIDEOfficial/AndroidIDE?include_prereleases&amp;label=latest%20release" alt="Latest release">

<!-- License -->
<img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License">
</p>

## 交流频道

### QQ群：524317060

## Android Sdk

现已支持`35.0.0`

感谢`lzhiyong`构建的工具包：[Termux Ndk](https://github.com/lzhiyong/termux-ndk)

现已修复适配`AndroidIDE`，请移步[Android Sdk Tools](https://github.com/MiyazKaori/android-sdk-tools)安装

### 更新日志

#### v2.7.1-beta-rev-1.0

- 修复部分闪退错误

#### 2.7.1+44105ee

- 新增功能：通过`Shizuku`进行静默安装，可在设置：构建运行 里面打开

#### 2.7.1+38687d7

> [!WARNING]
>
> 此版本由于`Sdk`，`Gradle`，`AGP`版本升级，需要使用构建工具包的版本为`35.0.0`，请自行配置
> 
> 此版本主要适配 `Androidx(Java)`，并未对`Kotlin`及其他项目模板进行适配
>
> 创建模块功能并未进行严格检测，请你始终确保`模块名称`及`模块包名`合规

- Androidx 项目模板升级
    - Gradle: 8.11.1
    - AGP: 8.9.1
    - Android Sdk: 36
    - Java: 17

- 新增功能：创建`Library Module`，可以长按文件树进行创建模块
    - Android Sdk: 36
    - Min Sdk: 26
    - Java: 17

## License

```
AndroidIDE is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

AndroidIDE is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
```

Any violations to the license can be reported either by opening an issue or writing a mail to us
directl