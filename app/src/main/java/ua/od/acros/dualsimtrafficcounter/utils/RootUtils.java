package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RootUtils {
    private static Handler mHandler;

    public static boolean canRunRootCommands(final Context context) {
        boolean result;
        Process suProcess;
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.arg1) {
                    case 0:
                        Toast.makeText(context, "Can't get root access or denied by user", Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        Toast.makeText(context, "Root access granted", Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        Toast.makeText(context, "Root access rejected for current UID", Toast.LENGTH_LONG).show();
                        break;
                    case 3:
                        Toast.makeText(context, "Probably, your device is not rooted", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
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
                //0
                mHandler.sendMessage(mHandler.obtainMessage(0));
            } else if (currUid.contains("uid=0")) {
                result = true;
                exitSu = true;
                //1
                mHandler.sendMessage(mHandler.obtainMessage(1));
            } else {
                result = false;
                exitSu = true;
                //2
                mHandler.sendMessage(mHandler.obtainMessage(2));
            }
            if (exitSu) {
                os.writeBytes("exit\n");
                os.flush();
            }
        } catch (Exception e) {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted
            result = false;
            //3
            mHandler.sendMessage(mHandler.obtainMessage(3));
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
