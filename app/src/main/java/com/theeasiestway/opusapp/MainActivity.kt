package com.theeasiestway.opusapp

import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.theeasiestway.opus.Constants
import com.theeasiestway.opus.Opus
import com.theeasiestway.opusapp.Logger.d
import com.theeasiestway.opusapp.Logger.i
import com.theeasiestway.opusapp.Logger.v
import com.theeasiestway.opusapp.mic.ControllerAudio

//
// Created by Loboda Alexey on 21.05.2020.
//
class MainActivity : AppCompatActivity() {
    private val audioPermission = android.Manifest.permission.RECORD_AUDIO
    private val readPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
    private val writePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE

    private lateinit var vSampleRateSeek: SeekBar
    private lateinit var vSampleRate: TextView
    private lateinit var vPlay: Button
    private lateinit var vStop: Button
    private lateinit var vBytes: RadioButton
    private lateinit var vShorts: RadioButton
    private lateinit var vFloats: RadioButton
    private lateinit var vMono: RadioButton
    private lateinit var vStereo: RadioButton
    private lateinit var vConvert: CheckBox

    private val codec = Opus()
    private val APPLICATION = Constants.Application.audio()
    private var CHUNK_SIZE = 0
    private lateinit var SAMPLE_RATE: Constants.SampleRate
    private lateinit var CHANNELS: Constants.Channels
    private lateinit var DEF_FRAME_SIZE: Constants.FrameSize
    private lateinit var FRAME_SIZE_BYTE: Constants.FrameSize
    private lateinit var FRAME_SIZE_SHORT: Constants.FrameSize
    private lateinit var FRAME_SIZE_FLOAT: Constants.FrameSize

    private var runLoop = false
    private var needToConvert = false

    override fun onCreate(savedInstanceState: Bundle?) {
        i { "onCreate" }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vSampleRateSeek = findViewById(R.id.vSampleRateSeek)
        vSampleRate = findViewById(R.id.vSampleRate)

        vPlay = findViewById(R.id.vPlay)
        vStop = findViewById(R.id.vStop)

        vBytes = findViewById(R.id.vHandleBytes)
        vShorts = findViewById(R.id.vHandleShorts)
        vFloats = findViewById(R.id.vHandleFloats)
        vMono = findViewById(R.id.vMono)
        vStereo = findViewById(R.id.vStereo)
        vConvert = findViewById(R.id.vConvert)

        vSampleRateSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                SAMPLE_RATE = getSampleRate(progress)
                val lableText = "${SAMPLE_RATE.v} Hz"
                vSampleRate.text = lableText
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        vSampleRateSeek.progress = 0

        vPlay.setOnClickListener {
            vPlay.visibility = View.GONE
            vStop.visibility = View.VISIBLE
            requestPermissions()
        }

        vStop.setOnClickListener {
            stopRecording()
        }

        vConvert.setOnCheckedChangeListener { _, isChecked -> needToConvert = isChecked }
    }

    private fun recalculateCodecValues() {
        DEF_FRAME_SIZE = getDefaultFrameSize(SAMPLE_RATE.v)
        CHANNELS = if (vMono.isChecked) Constants.Channels.mono() else Constants.Channels.stereo()
        /** "CHUNK_SIZE = DEF_FRAME_SIZE.v * CHANNELS.v * 2" it's formula from opus.h "frame_size*channels*sizeof(opus_int16)" */
        CHUNK_SIZE =
            DEF_FRAME_SIZE.v * CHANNELS.v * 2                                       // bytes or shorts or floats in a frame
        FRAME_SIZE_BYTE =
            Constants.FrameSize.fromValue(CHUNK_SIZE / 2 / CHANNELS.v)        // samples per channel
        FRAME_SIZE_SHORT =
            Constants.FrameSize.fromValue(CHUNK_SIZE / CHANNELS.v)            // samples per channel
        FRAME_SIZE_FLOAT = Constants.FrameSize.fromValue(160)
        //Constants.FrameSize.fromValue(CHUNK_SIZE * 2 / CHANNELS.v)        // samples per channel
    }

    private fun getSampleRate(v: Int): Constants.SampleRate {
        return when (v) {
            0 -> Constants.SampleRate._8000()
            1 -> Constants.SampleRate._12000()
            2 -> Constants.SampleRate._16000()
            3 -> Constants.SampleRate._24000()
            4 -> Constants.SampleRate._48000()
            else -> throw IllegalArgumentException()
        }
    }

    private fun getDefaultFrameSize(v: Int): Constants.FrameSize {
        return when (v) {
            8000 -> Constants.FrameSize._160()
            12000 -> Constants.FrameSize._240()
            16000 -> Constants.FrameSize._160()
            24000 -> Constants.FrameSize._240()
            48000 -> Constants.FrameSize._120()
            else -> throw IllegalArgumentException()
        }
    }

    private fun stopRecording() {
        vStop.visibility = View.GONE
        vPlay.visibility = View.VISIBLE
        stopLoop()
        ControllerAudio.stopRecord()
        ControllerAudio.stopTrack()
        vSampleRateSeek.isEnabled = true
        vBytes.isEnabled = true
        vShorts.isEnabled = true
        vFloats.isEnabled = true
        vMono.isEnabled = true
        vStereo.isEnabled = true
    }

    private fun startLoop() {
        d { "startLoop" }
        stopLoop()

        vSampleRateSeek.isEnabled = false
        vBytes.isEnabled = false
        vShorts.isEnabled = false
        vFloats.isEnabled = false
        vMono.isEnabled = false
        vStereo.isEnabled = false

        val handleBytes = vBytes.isChecked
        val handleShorts = vShorts.isChecked
        val handleFloats = vFloats.isChecked
        d { "startLoop b=$handleBytes s=$handleShorts f=$handleFloats" }
        recalculateCodecValues()

        codec.encoderCreate(SAMPLE_RATE, CHANNELS, APPLICATION)
        codec.decoderCreate(SAMPLE_RATE, CHANNELS)

        val frameSize = when {
            handleBytes -> FRAME_SIZE_SHORT.v //FRAME_SIZE_BYTE.v
            handleShorts -> FRAME_SIZE_SHORT.v
            handleFloats -> FRAME_SIZE_FLOAT.v
            else -> CHUNK_SIZE
        }
        val pcm = when {
            handleBytes -> AudioFormat.ENCODING_PCM_16BIT
            handleShorts -> AudioFormat.ENCODING_PCM_16BIT
            handleFloats -> AudioFormat.ENCODING_PCM_FLOAT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }


        ControllerAudio.initRecorder(SAMPLE_RATE.v, frameSize, pcm, CHANNELS.v == 1)
        ControllerAudio.initTrack(SAMPLE_RATE.v, CHANNELS.v == 1)
        ControllerAudio.startRecord()
        runLoop = true
        Thread {
            while (runLoop) {
                when {
                    handleBytes -> handleBytes()
                    handleShorts -> handleShorts()
                    handleFloats -> handleFloats()
                }
                d { "handle ???" }
                runLoop = false//FIXIT
            }
            if (!runLoop) {
                codec.encoderRelease()
                codec.decoderRelease()
            }
        }.start()
    }

    private fun stopLoop() {
        runLoop = false
    }

    private fun handleFloats() {
        d { "handleFloats fs=${FRAME_SIZE_FLOAT.v}" }
        val frame = ControllerAudio.getFrameFloat() ?: return
//        val shorts = ByteArray(frame.size)
//        for ((id, float) in frame.withIndex()) {
//            shorts[id] = (Byte.MAX_VALUE * float.coerceIn(-1.0f, 1.0f)
//                .toShort()).toByte() //TODO to converter
//            d { "handleFloats f=$float b=${shorts[id]}" }
//        }

        val encoded = codec.encode(frame, FRAME_SIZE_FLOAT) ?: return
        v { "encoded: ${frame.size} floats of ${if (CHANNELS.v == 1) "MONO" else "STEREO"} audio into ${encoded.size} bytes" }
        val decoded = FloatArray(frame.size)
        codec.decodeFloat(encoded, encoded.size, decoded, FRAME_SIZE_FLOAT)
        v { "decoded: ${decoded.size} floats" }

//        if (needToConvert) {
//            val converted = codec.convert(decoded) ?: return
//            Log.d(TAG, "converted: ${decoded.size} shorts into ${converted.size} bytes")
//            ControllerAudio.write(converted)
//        } else ControllerAudio.write(decoded)
        v { "===========================================" }
    }

    private fun handleShorts() {
        d { "handleShorts fs=${FRAME_SIZE_SHORT.v}" }
        val frame = ControllerAudio.getFrameShort() ?: return
        val encoded = codec.encode(frame, FRAME_SIZE_SHORT) //?: return
        if (encoded == null) {
            d { "handleShorts NULL" }
            return
        }
        v { "encoded: ${frame.size} shorts of ${if (CHANNELS.v == 1) "MONO" else "STEREO"} audio into ${encoded.size} shorts" }
        val decoded = codec.decode(encoded, FRAME_SIZE_SHORT) ?: return
        v { "decoded: ${decoded.size} shorts" }

        if (needToConvert) {
            val converted = codec.convert(decoded) ?: return
            v { "converted: ${decoded.size} shorts into ${converted.size} bytes" }
            ControllerAudio.write(converted)
        } else ControllerAudio.write(decoded)
        v { "===========================================" }
    }

    private fun handleBytes() {
        d { "handleBytes fs=${FRAME_SIZE_BYTE.v}" }
        val frame = ControllerAudio.getFrame() ?: return
        val encoded = codec.encode(frame, FRAME_SIZE_BYTE) ?: return
        v { "encoded: ${frame.size} bytes of ${if (CHANNELS.v == 1) "MONO" else "STEREO"} audio into ${encoded.size} bytes" }
        val decoded = codec.decode(encoded, FRAME_SIZE_BYTE) ?: return
        v { "decoded: ${decoded.size} bytes" }

        if (needToConvert) {
            val converted = codec.convert(decoded) ?: return
            v { "converted: ${decoded.size} bytes into ${converted.size} shorts" }
            ControllerAudio.write(converted)
        } else ControllerAudio.write(decoded)
        v { "===========================================" }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) startLoop()
        else if (checkSelfPermission(audioPermission) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(readPermission) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(writePermission) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(audioPermission, readPermission, writePermission), 123)
        } else startLoop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        i { "onRequestPermissionsResult" }
        if (permissions[0] == audioPermission &&
            permissions[1] == readPermission &&
            permissions[2] == writePermission &&
            requestCode == 123
        ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                grantResults[2] == PackageManager.PERMISSION_GRANTED
            ) startLoop()
            else Toast.makeText(
                this,
                "App doesn't have enough permissions to continue",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onPause() {
        super.onPause()
        stopRecording()
    }
}
