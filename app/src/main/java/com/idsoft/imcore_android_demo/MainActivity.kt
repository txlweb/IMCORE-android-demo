package com.idsoft.imcore_android_demo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.idsoft.imcore_android_demo.ui.theme.IMCOREandroiddemoTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import src.com.textreader.comic.ComicMake
import src.settings
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors


class MainActivity : ComponentActivity() {
    val ACTION_SIMULATE_BUTTON_CLICK = "impfl";
    var btn_up: Unit? = null;
    var vw: WebView? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        ct.ct = this
        val permissions =  arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET)
        this.requestPermissions(permissions,101)
        for (str in permissions) {
            if (this.checkSelfPermission(str) !== PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(permissions, 101)
                this.requestPermissions(permissions,101)
                this.requestPermissions(permissions,101)
            }
        }

        val ex_path = this.getExternalFilesDir(null)?.path
        src.settings.tmp_path=ex_path+"/"
        src.settings.sync_path=ex_path+"/rom"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            GlobalScope.launch {
                src.HTTP_SERVER.run()
            }
            GlobalScope.launch {
                while (true){
                    if(ct.us){
                        ct.us=false;
                        importFile()
                    }
                }
            }
        }

        super.onCreate(savedInstanceState)
        setContent {
            IMCOREandroiddemoTheme {
                // A surface container using the 'background' color from the theme
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                        vw = WebView(this)
                        vw!!.webViewClient = object : WebViewClient() {
                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                url?.let { view?.loadUrl(it) }
                                return true
                            }
                        }
                        vw!!.settings.javaScriptEnabled = true
                        vw!!.loadUrl("http://127.0.0.1:8080")
                        val layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        setContentView(vw)
                        vw!!.layoutParams = layoutParams
                        Button(onClick = { vw!!.loadUrl("http://127.0.0.1:8080") }) {
                            Text(text = "LOAD")
                        }

                    }else{
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Text(text = "!! ERROR !! only support android O+ version.")
                        }
                    }
            }
        }
    }

    fun importFile(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("*/*") // 接受任何类型的文件
        startActivityForResult(intent, 0)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            val uri = data?.data // 获取选择的文件的URI
            try {
                if (uri != null) {
                    File(src.settings.sync_path).mkdir()
                    copyFileFromUri(uri, src.settings.sync_path+"/"+getFileNameFromUri(uri))
                    vw?.loadUrl("http://127.0.0.1:8080/build.html");
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    private fun getFileNameFromUri(uri: Uri): String {
        var fileName: String? = null
        val path = uri.path
        if (path != null) {
            // 尝试从路径中提取文件名
            val cut = path.lastIndexOf('/')
            fileName = if (cut != -1) {
                path.substring(cut + 1)
            } else {
                path
            }
        } else {
            // 如果路径为空，可能是内容提供者，尝试查询数据库
            val cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                } finally {
                    cursor.close()
                }
            }
        }
        fileName = fileName!!.replace("primary:".toRegex(), "") //有多的东西
        fileName = fileName.replace("msf:".toRegex(), "") //有多的东西
        if(fileName.substring(fileName.length-4,fileName.length) != "CEIP"){
            fileName=fileName+".zip"
        }
        return fileName
    }
    private fun copyFileFromUri(uri: Uri, destinationPath: String): Boolean {
        val contentResolver = contentResolver
        val assetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r") ?: return false
        val sourceFileDescriptor = assetFileDescriptor.createInputStream()
        val destinationStream = FileOutputStream(destinationPath)
        val buffer = ByteArray(1024)
        var length: Int
        while (sourceFileDescriptor.read(buffer).also { length = it } != -1) {
            destinationStream.write(buffer, 0, length)
        }
        destinationStream.flush()
        sourceFileDescriptor.close()
        assetFileDescriptor.close()
        destinationStream.close()
        return true
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    vw?.goBack()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

}

