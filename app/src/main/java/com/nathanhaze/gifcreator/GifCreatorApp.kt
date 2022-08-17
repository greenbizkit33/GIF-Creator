package com.nathanhaze.gifcreator

import android.app.Application
import android.os.Environment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.io.File
import java.util.*

class GifCreatorApp : Application() {

    override fun onCreate() {
        super.onCreate()

        //MobileAds.initialize(this, getResources().getString(R.string.admob_account));
//        MobileAds.initialize(this, object : OnInitializationCompleteListener {
//            override fun onInitializationComplete(initializationStatus: InitializationStatus?) {}
//        })

        MobileAds.initialize(this) {}
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(Arrays.asList("0135F7F684D504931784BE16DAA97AF8")).build()
        MobileAds.setRequestConfiguration(configuration)

    }

    fun getParentFolder(): File? {
        val path = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/GifCreator/"
        )
        if (!path.exists()) {
            path.mkdirs()
        }
        return path
    }

    fun getListFiles(): List<String?>? {
        val fileList: Array<File> = getParentFolder()?.listFiles()
            ?: return emptyList()
        val length = fileList.size // null pointer
        val stringList: MutableList<String> = mutableListOf()
        for (i in 0 until length) {
            stringList.add(fileList[i].absolutePath)
        }
        return stringList
    }
}