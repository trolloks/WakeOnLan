 package me.trolloks.wakeonlan;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

 public class HomeActivity extends AppCompatActivity {

     public static final String PREFS_NAME = "WakeSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppCompatEditText editText = (AppCompatEditText)findViewById(R.id.mac);
                AppCompatEditText ip = (AppCompatEditText)findViewById(R.id.ip);
                String mac = editText.getText().toString();
                if (!macValidate(mac)) {
                    editText.setError("Invalid Mac Address");
                    return;
                }

                // set to cache
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("ip", ip.getText().toString());
                editor.putString("mac", mac);
                editor.apply();

                sendWake(mac, ip.getText().toString());
            }
        });

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        AppCompatEditText editText = (AppCompatEditText)findViewById(R.id.mac);
        AppCompatEditText ip = (AppCompatEditText)findViewById(R.id.ip);
        editText.setText(settings.getString("mac", ""));
        ip.setText(settings.getString("ip", ""));
    }

     public void sendWake(final String mac, final String ip) {
         final byte[] magicPacket = createMagicPacket(getMacBytes(mac));

         Executors.newFixedThreadPool(1).submit(new Runnable() {
             @Override
             public void run() {
                 try {
                     // create socket to IP
                     final InetAddress address = InetAddress.getByName(ip);
                     final DatagramPacket packet = new DatagramPacket(magicPacket, magicPacket.length, address, 9);
                     final DatagramSocket socket = new DatagramSocket();
                     socket.send(packet);
                     socket.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         });
         Toast.makeText(this, "Sending Magic Packet to " + ip, Toast.LENGTH_SHORT).show();

     }

     private byte[] createMagicPacket(byte[] macBytes){
         byte [] magicPacket = new byte[6 + (16 * macBytes.length)];
         for (int i = 0; i < 22; i ++){
             if (i < 6)
                 magicPacket[i] = (byte) 0xFF;
             else{
                 System.arraycopy(macBytes, 0, magicPacket, 6 + ((i - 6) * macBytes.length), macBytes.length);
             }
         }
         return magicPacket;
     }

     private byte[] getMacBytes(String mac) {
         String[] splitMac = mac.split("[:-]");
         byte [] rawMac = new byte [splitMac.length];
         for (int i = 0 ; i < rawMac.length; i++){
             rawMac[i] = (byte) (Integer.parseInt(splitMac[i],16) & 0xff);
         }
         return rawMac;
     }


     public boolean macValidate(String mac) {
        Pattern p = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
        Matcher m = p.matcher(mac);
        return m.find();
    }
}
