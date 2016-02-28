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
    static class MyHandler extends Handler {
        private final Context mContext;

        MyHandler(Context context) {
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case 0:
                    Toast.makeText(mContext, "Can't get root access or denied by user", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(mContext, "Root access granted", Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(mContext, "Root access rejected for current UID", Toast.LENGTH_LONG).show();
                    break;
                case 3:
                    Toast.makeText(mContext, "Probably, your device is not rooted", Toast.LENGTH_LONG).show();
                    break;
                case 4:
                    Toast.makeText(mContext, "Error executing root action", Toast.LENGTH_LONG).show();
                    break;
                case 5:
                    Toast.makeText(mContext, "Can't get root access", Toast.LENGTH_LONG).show();
                    break;
                case 6:
                    Toast.makeText(mContext, "Error executing internal operation", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    public static boolean canRunRootCommands(final Context context) {
        boolean result;
        Process suProcess;
        MyHandler handler = new MyHandler(context);
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
                handler.sendMessage(handler.obtainMessage(0));
            } else if (currUid.contains("uid=0")) {
                result = true;
                exitSu = true;
                //1
                handler.sendMessage(handler.obtainMessage(1));
            } else {
                result = false;
                exitSu = true;
                //2
                handler.sendMessage(handler.obtainMessage(2));
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
            handler.sendMessage(handler.obtainMessage(3));
        }
        return result;
    }

    public static boolean executeAsRoot(Context context, ArrayList<String> commands) {
        boolean result = false;
        MyHandler handler = new MyHandler(context);
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
                    //4
                    handler.sendMessage(handler.obtainMessage(4));
                }
            }
        } catch (IOException ex) {
            //5
            handler.sendMessage(handler.obtainMessage(5));
        } catch (SecurityException ex) {
            //5
            handler.sendMessage(handler.obtainMessage(5));
        } catch (Exception ex) {
            //6
            handler.sendMessage(handler.obtainMessage(6));
        }
        return result;
    }
}
