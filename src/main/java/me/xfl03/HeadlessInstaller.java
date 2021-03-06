/*
 * Installer
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package me.xfl03;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraftforge.installer.DownloadUtils;
import net.minecraftforge.installer.InstallerPanel;
import net.minecraftforge.installer.actions.Actions;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

import javax.swing.*;
import java.io.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

public class HeadlessInstaller {
    public static boolean headless = false;

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        //Modified
        OptionSpec<Void> progressOption = parser.accepts("progress", "Show progress only in console");
        OptionSpec<File> clientInstallOption = parser.accepts("installClient", "Install a client to the specified directory").withOptionalArg().ofType(File.class).defaultsTo(new File("."));

        OptionSpec<File> serverInstallOption = parser.accepts("installServer", "Install a server to the specified directory").withOptionalArg().ofType(File.class).defaultsTo(new File("."));
        OptionSpec<File> extractOption = parser.accepts("extract", "Extract the contained jar file to the specified directory").withOptionalArg().ofType(File.class).defaultsTo(new File("."));
        OptionSpec<Void> helpOption = parser.acceptsAll(Arrays.asList("h", "help"), "Help with this installer");
        OptionSpec<Void> offlineOption = parser.accepts("offline", "Don't attempt any network calls");
        OptionSet optionSet = parser.parse(args);

        ProgressCallback monitor;
        try {
            if (optionSet.has(progressOption)) {
                monitor = getProgressDisplayCallback(ProgressCallback.withOutputs(getLog()), System.out);
            } else {
                monitor = ProgressCallback.withOutputs(System.out, getLog());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            monitor = ProgressCallback.withOutputs(System.out);
        }
        hookStdOut(monitor);

        if (System.getProperty("java.net.preferIPv4Stack") == null) //This is a dirty hack, but screw it, i'm hoping this as default will fix more things then it breaks.
        {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        String vendor = System.getProperty("java.vendor", "missing vendor");
        String javaVersion = System.getProperty("java.version", "missing java version");
        String jvmVersion = System.getProperty("java.vm.version", "missing jvm version");
        monitor.message(String.format("JVM info: %s - %s - %s", vendor, javaVersion, jvmVersion));
        monitor.message("java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));

        String path = HeadlessInstaller.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (path.contains("!/")) {
            monitor.stage("Due to java limitation, please do not run this jar in a folder ending with !");
            monitor.message(path);
            return;
        }

        if (optionSet.has(helpOption)) {
            parser.printHelpOn(System.out);
            return;
        }

        int cnt = 0;
        if (optionSet.has(offlineOption)) {
            DownloadUtils.OFFLINE_MODE = true;
            monitor.message("ENABELING OFFLINE MODE");
            cnt = 1;
        } else {
            FixSSL.fixup(monitor);
        }

        Actions action = null;
        File target = null;

        //Modified
        if (optionSet.has(clientInstallOption)) {
            action = Actions.CLIENT;
            target = optionSet.valueOf(clientInstallOption);
        } else if (optionSet.has(serverInstallOption)) {
            action = Actions.SERVER;
            target = optionSet.valueOf(serverInstallOption);
        } else if (optionSet.has(extractOption)) {
            action = Actions.EXTRACT;
            target = optionSet.valueOf(extractOption);
        }

        if (action != null) {
            try {
                HeadlessInstaller.headless = true;
                monitor.message("Target Directory: " + target);
                Install install = Util.loadInstallProfile();
                if (install.getSpec() != 0) {
                    monitor.message("Invalid launcher profile spec: " + install.getSpec() + " Only 0 is supported");
                    monitor.stage("INSTALL FAILED");
                    System.exit(1);
                }
                if (!action.getAction(install, monitor).run(target, a -> true)) {
                    monitor.stage("INSTALL FAILED");
                    System.exit(1);
                } else {
                    monitor.message(action.getSuccess(install.getPath().getName()));
                    monitor.stage("INSTALL SUCCESSFUL");
                }
                System.exit(0);
            } catch (Throwable e) {
                monitor.stage("A problem installing was detected, install cannot continue");
                System.exit(1);
            }
        } else if (optionSet.specs().size() > cnt)
            parser.printHelpOn(System.err);
        else
            launchGui(monitor);
    }

    private static File getMCDir() {
        String userHomeDir = System.getProperty("user.home", ".");
        String osType = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        String mcDir = ".minecraft";
        if (osType.contains("win") && System.getenv("APPDATA") != null)
            return new File(System.getenv("APPDATA"), mcDir);
        else if (osType.contains("mac"))
            return new File(new File(new File(userHomeDir, "Library"), "Application Support"), "minecraft");
        return new File(userHomeDir, mcDir);
    }

    private static void launchGui(ProgressCallback monitor) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        Install profile = Util.loadInstallProfile();
        InstallerPanel panel = new InstallerPanel(getMCDir(), profile);
        panel.run(monitor);
    }

    private static OutputStream getLog() throws FileNotFoundException {
        File f = new File(HeadlessInstaller.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        File output;
        if (f.isFile()) output = new File(f.getName() + ".log");
        else output = new File("installer.log");

        return new BufferedOutputStream(new FileOutputStream(output));
    }

    static void hookStdOut(ProgressCallback monitor) {
        final Pattern endingWhitespace = Pattern.compile("\\r?\\n$");
        final OutputStream monitorStream = new OutputStream() {

            @Override
            public void write(byte[] buf, int off, int len) {
                byte[] toWrite = new byte[len];
                System.arraycopy(buf, off, toWrite, 0, len);
                write(toWrite);
            }

            @Override
            public void write(byte[] b) {
                String toWrite = new String(b);
                toWrite = endingWhitespace.matcher(toWrite).replaceAll("");
                if (!toWrite.isEmpty()) {
                    monitor.message(toWrite);
                }
            }

            @Override
            public void write(int b) {
                write(new byte[]{(byte) b});
            }
        };

        System.setOut(new PrintStream(monitorStream));
        System.setErr(new PrintStream(monitorStream));
    }

    static ProgressCallback getProgressDisplayCallback(ProgressCallback parent, OutputStream stdout) {
        return new ProgressCallback() {
            @Override
            public void message(String message, MessagePriority priority) {
                parent.message(message, priority);
            }

            @Override
            public void start(String label) {
                writeStdout("START: " + label);
            }

            @Override
            public void stage(String label) {
                writeStdout("STAGE: " + label);
            }

            @Override
            public void progress(double progress) {
                String message = String.format("%.1f", progress * 100);
                writeStdout(message);
            }

            private void writeStdout(String message) {
                message = message + "\n";
                try {
                    stdout.write(message.getBytes());
                    stdout.flush();
                } catch (Exception exception) {
                    message(exception.toString(), MessagePriority.HIGH);
                }
            }
        };
    }
}
