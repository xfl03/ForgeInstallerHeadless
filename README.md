# ForgeInstallerHeadless
Add client CLI install for Forge Installer.
## Notice
Forge Installer 2.0 only, which is used in 1.13+.
## Usage
```shell script
java -cp "forge-installer-headless.jar;forge-installer.jar" me.xfl03.HeadlessInstaller -installClient PATH
```
Example for using `forge-installer-headless-1.0.0` and installing `forge-1.15.2-31.1.0` to `%appdata%/.minecraft`:
```shell script
java -cp "forge-installer-headless-1.0.0.jar;forge-1.15.2-31.1.0-installer.jar" me.xfl03.HeadlessInstaller -installClient %appdata%/.minecraft
```
