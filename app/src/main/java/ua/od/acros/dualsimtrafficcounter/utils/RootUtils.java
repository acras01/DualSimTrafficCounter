package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RootUtils {
    public static boolean canRunRootCommands(Context context) {
        boolean result;
        Process suProcess;
        try {
            suProcess = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            InputStreamReader is = new InputStreamReader(suProcess.getInputStream());
            BufferedReader br = new BufferedReader(is);
            // Getting the id of the current user to check if this is root
            os.writeBytes("id\n");
            os.flush();
            String currUid = br.readLine();
            boolean exitSu;
            if (currUid == null) {
                result = false;
                exitSu = false;
                Toast.makeText(context, "Can't get root access or denied by user", Toast.LENGTH_LONG).show();
            } else if (currUid.contains("uid=0")) {
                result = true;
                exitSu = true;
                Toast.makeText(context, "Root access granted", Toast.LENGTH_LONG).show();
            } else {
                result = false;
                exitSu = true;
                Toast.makeText(context, "Root access rejected: " + currUid, Toast.LENGTH_LONG).show();
            }
            if (exitSu) {
                os.writeBytes("exit\n");
                os.flush();
            }
        } catch (Exception e) {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted
            result = false;
            Toast.makeText(context, "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return result;
    }

    public static boolean executeAsRoot(Context context, ArrayList<String> commands) {
        boolean result = false;
        try {
            if (commands != null && commands.size() > 0) {
                Process suProcess = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                // Execute commands that require root access
                for (String currCommand : commands) {
                    os.writeBytes(currCommand + "\n");
                    os.flush();
                }
                os.writeBytes("exit\n");
                os.flush();
                try {
                    result = suProcess.waitFor() != 255;
                }
                catch (Exception ex) {
                    Toast.makeText(context, "Error executing root action [" + ex.getClass().getName() + "] : " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException ex) {
            Toast.makeText(context, "Can't get root access [" + ex.getClass().getName() + "] : " + ex.getMessage(), Toast.LENGTH_LONG).show();
        } catch (SecurityException ex) {
            Toast.makeText(context, "Can't get root access [" + ex.getClass().getName() + "] : " + ex.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(context, "Error executing internal operation [" + ex.getClass().getName() + "] : " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        return result;
    }
}
