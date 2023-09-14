package com.example.bigfilefinder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity                     
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : Activity() {
    private var bigFileFinder = BigFileFinder(0)
    private lateinit var increaseButton: Button
    private lateinit var decreaseButton: Button
    private lateinit var countTextView: TextView
    private lateinit var findButton: Button
    private lateinit var addButton: Button
    private lateinit var directoriesListButton: Button
    private lateinit var filesListButton: Button
    private lateinit var listView: ListView
    private var notificationManager: NotificationManager? = null

    private val TAG = "Permission"
    private val READ_REQUEST_CODE = 101
    private val OPEN_FILE_CODE = 100
    private val NOTIFICATION_REQUEST_CODE = 102
    private val CHANNEL_ID = "com.bigfilefinder.notify.bigfiles"
    private var paths = mutableListOf<String>()
    private var directoriesListActive = true

    private lateinit var directoriesAdapter: ArrayAdapter<String>
    private lateinit var filesAdapter: SimpleAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeProperties()
        setButtonsListeners()
        setupPermissions()
    }

    private fun initializeProperties() {
        increaseButton = findViewById(R.id.increase_button)
        decreaseButton = findViewById(R.id.decrease_button)
        countTextView = findViewById(R.id.count_of_biggest_files)
        findButton = findViewById(R.id.find_button)
        addButton = findViewById(R.id.add_button)
        directoriesListButton  = findViewById(R.id.button_to_list_of_directories)
        filesListButton  = findViewById(R.id.button_to_list_of_files)
        listView = findViewById(R.id.list_of_directories)
        directoriesAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        val data = mutableListOf(mapOf("path" to "", "size" to ""))
        filesAdapter = SimpleAdapter(this, data, android.R.layout.simple_list_item_2, arrayOf("path", "size"), arrayOf(android.R.id.text1, android.R.id.text2).toIntArray())
        listViewChangeAdapter(directoriesAdapter, true)
        switchButton(directoriesListButton, filesListButton)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(CHANNEL_ID, "Notify big files", "Example News Channel")
    }

    private fun setButtonsListeners() {
        increaseButton.setOnClickListener {
            bigFileFinder.increaseCount()
            countTextView.text = bigFileFinder.count.toString()
        }

        decreaseButton.setOnClickListener {
            bigFileFinder.decreaseCount()
            countTextView.text = bigFileFinder.count.toString()
        }

        findButton.setOnClickListener {
            bigFileFinder.addDirectories(paths)
            val bigFiles = bigFileFinder.findBigFiles()
            fillFilesAdapter(bigFiles)
            sendNotificatons(bigFiles)
            listViewChangeAdapter(filesAdapter, false)
            switchButton(filesListButton, directoriesListButton)
        }

        addButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, OPEN_FILE_CODE)
        }

        listView.setOnItemLongClickListener { adapterView, view, index, l ->
            if (directoriesListActive) {
                val deleteValue = directoriesAdapter.getItem(index)
                directoriesAdapter.remove(deleteValue)
                paths.remove(deleteValue)
            }
            true
        }

        directoriesListButton.setOnClickListener {
            listViewChangeAdapter(directoriesAdapter, true)
            switchButton(directoriesListButton, filesListButton)
        }

        filesListButton.setOnClickListener {
            listViewChangeAdapter(filesAdapter, false)
            switchButton(filesListButton, directoriesListButton)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == OPEN_FILE_CODE && resultCode == RESULT_OK) {
            resultData?.data?.also { uri ->
                val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                val path = getRealPathFromURI(this, docUri).toString()
                if (!paths.contains(path)) {
                    paths.add(path)
                    directoriesAdapter.add(path)
                }
            }
        }
    }

    private fun fillFilesAdapter(files: List<MyFile>) {
        val pairs = mutableListOf<Map<String, String>>()
        for (i in files.indices) {
            pairs.add(mapOf("path" to files[i].path, "size" to "Size: " + files[i].size.toString() + " KB"))
        }
        filesAdapter = SimpleAdapter(this, pairs, android.R.layout.simple_list_item_2, arrayOf("path", "size"), arrayOf(android.R.id.text1, android.R.id.text2).toIntArray())
    }

    private fun setupPermissions() {
        val permissionES = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionES != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_REQUEST_CODE)
        }

        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                }
            }
        }
        when (requestCode) {
            NOTIFICATION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText( this, "Notification permission required", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun listViewChangeAdapter(adapter: ListAdapter, isDirectories: Boolean){
        listView.adapter = adapter
        directoriesListActive = isDirectories
    }

    private fun sendNotificatons(files: List<MyFile>){
        for (i in files.indices) {
            val notification = Notification.Builder(
                this@MainActivity,
                CHANNEL_ID
            )
                .setContentTitle(files[i].path)
                .setContentText(files[i].size.toString())
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setChannelId(CHANNEL_ID)
                .setNumber(bigFileFinder.count)
                .build()
            notificationManager?.notify(i, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(id: String, name: String, description: String) {
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        channel.enableLights(true)
        channel.lightColor = R.color.secondary2
        channel.enableVibration(true)
        channel.vibrationPattern =
            longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager?.createNotificationChannel(channel)
    }

    @SuppressLint("ResourceAsColor")
    private fun switchButton(button1: Button, button2: Button){
        button1.setBackgroundResource(R.drawable.list_button_active)
        button2.setBackgroundResource(R.drawable.list_button_inactive)
        button1.setTextColor(R.color.black)
        button2.setTextColor(R.color.white)
    }
}