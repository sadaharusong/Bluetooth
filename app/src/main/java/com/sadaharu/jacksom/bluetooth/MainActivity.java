package com.sadaharu.jacksom.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private BluetoothAdapter mBluetoothAdapter;
    private ListView lvDevices;

    private List<String> bluetoothDevices = new ArrayList<>();

    private ArrayAdapter<String> arrayAdapter;

    private final UUID MY_UUID = UUID.randomUUID();
    private final String NAME = "Bluetooth_Socket";

    private BluetoothSocket clientSocket;
    private BluetoothDevice device;
    private AcceptThread acceptThread;
    private OutputStream os;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      /*//蓝牙的打开方式一：
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent,1);

        //蓝牙的打开方式二：
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        //调用enable就把蓝牙打开
        adapter.enable();
        //调用disable就把蓝牙关闭
        adapter.disable();*/

        lvDevices = (ListView) findViewById(R.id.lvDevices);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        acceptThread = new AcceptThread();
        acceptThread.start();

        //已经配过对的，存储在这个集合里
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        //将已经配对过的设备显示出来
        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                bluetoothDevices.add(device.getName() + ":" + device.getAddress());
            }
        }

        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,
                android.R.id.text1,bluetoothDevices);

        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(this);

        //找到一个设备会发送广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        //全部搜索完成会发送一个广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);
    }

    //按钮监听方法
    public void onClick_Search(View view)
    {
        //设置一个进度条显示正在扫描
        setProgressBarVisibility(true);
        setTitle("正在扫描……");
        if (mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //如果找到这个设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //获得这个设备的信息
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //如果没有被配对过，再添加
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    bluetoothDevices.add(device.getName() + ":" + device.getAddress() + "\n");
                    arrayAdapter.notifyDataSetChanged();
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                setProgressBarVisibility(false);
                setTitle("搜索已经完成");
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String s = arrayAdapter.getItem(position);
        String address = s.substring(s.indexOf(":") + 1).trim();

        try{
            if (mBluetoothAdapter.isDiscovering())
            {
                mBluetoothAdapter.cancelDiscovery();
            }
            if (device == null)
            {
                device = mBluetoothAdapter.getRemoteDevice(address);
            }
            if (clientSocket == null)
            {
                clientSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                clientSocket.connect();

                os = clientSocket.getOutputStream();
            }

        }catch (Exception e )
        {

        }

        if (os != null)
        {
            try {
                os.write("发送信息到其他蓝牙设备".getBytes("utf-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //服务端
    private Handler handler = new Handler()
    {
        public void handlerMessage(Message msg)
        {
            Toast.makeText(MainActivity.this,String.valueOf(msg.obj),Toast.LENGTH_LONG).show();
            super.handleMessage(msg);
        }
    };

    private class AcceptThread extends Thread
    {
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;
        private InputStream is;
        private OutputStream os;
        public AcceptThread()
        {
            try {
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                socket = serverSocket.accept();
                is = socket.getInputStream();
                os = socket.getOutputStream();

                //有数据就从客户端读入，没有数据就阻塞
                while (true)
                {
                    byte[] buffer = new byte[128];
                    int count = is.read(buffer);
                    Message msg = new Message();
                    msg.obj = new String(buffer, 0,count,"utf-8");
                    handler.sendMessage(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
