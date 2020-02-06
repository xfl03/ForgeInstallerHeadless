# ForgeInstallerHeadless
Add client CLI install for Forge Installer.
## Notice
Forge Installer 2.0 only, which is used in 1.13+.
## Usage
```shell script
java -cp "forge-installer-headless.jar;forge-installer.jar" me.xfl03.HeadlessInstaller -installClient PATH
```
Example for using `forge-installer-headless-1.0.1` and installing `forge-1.15.2-31.1.0` to `%appdata%/.minecraft`:
```shell script
java -cp "forge-installer-headless-1.0.1.jar;forge-1.15.2-31.1.0-installer.jar" me.xfl03.HeadlessInstaller -installClient %appdata%/.minecraft
```
### Progress Display
If `-progress` parameter is added, console will show progress display only.  
`STAGE` means which stage has reached, `START` means which task is processing.  
After install, it will output `STAGE: INSTALL SUCCESSFUL` or `STAGE: INSTALL FAILED`.