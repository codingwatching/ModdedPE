package com.mcal.moddedpe

import android.os.Bundle
import com.mcal.moddedpe.task.CustomServers
import com.mcal.moddedpe.task.MapsInstaller
import com.mcal.moddedpe.task.NativeInstaller
import com.mcal.moddedpe.utils.ABIHelper
import com.mcal.moddedpe.utils.Patcher
import com.mojang.minecraftpe.MainActivity
import java.io.File

class GameActivity : MainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        CustomServers(this).install()
        MapsInstaller(this).install()
        NativeInstaller(this).install()
        patchNativeLibraryDir()
        loadLibraries()
        super.onCreate(savedInstanceState)
    }

    private fun patchNativeLibraryDir() {
        runCatching {
            Patcher.patchNativeLibraryDir(classLoader, File("${filesDir}/native/${ABIHelper.getABI()}/"))
        }
    }

    private fun loadLibraries() {
        System.loadLibrary("fmod")
        arrayListOf(
            "c++_shared",
            "minecraftpe",
            "MediaDecoders_Android"
        ).forEach {
            runCatching {
                System.loadLibrary(it)
            }
        }
    }
}
