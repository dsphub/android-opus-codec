package com.theeasiestway.opus

import android.util.Log


//
// Created by Loboda Alexey on 21.05.2020.
//

class Opus {
    companion object {
        const val TAG = "CodecOpus"

        init {
            try {
                System.loadLibrary("easyopus")
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't load opus library: $e")
            }
        }
    }

    //
    // Encoder
    //

    fun encoderCreate(
        sampleRate: Constants.SampleRate,
        channels: Constants.Channels,
        application: Constants.Application
    ): Int {
        return encoderCreate(sampleRate.v, channels.v, application.v)
    }

    private external fun encoderCreate(sampleRate: Int, numChannels: Int, application: Int): Int

    fun encoderInit(
        sampleRate: Constants.SampleRate,
        channels: Constants.Channels,
        application: Constants.Application
    ): Int {
        return encoderInit(sampleRate.v, channels.v, application.v)
    }

    private external fun encoderInit(sampleRate: Int, numChannels: Int, application: Int): Int

    fun encoderSetBitrate(bitrate: Constants.Bitrate): Int {
        return encoderSetBitrate(bitrate.v)
    }

    private external fun encoderSetBitrate(bitrate: Int): Int

    fun encoderSetComplexity(complexity: Constants.Complexity): Int {
        return encoderSetComplexity(complexity.v)
    }
    private external fun encoderSetComplexity(complexity: Int): Int

    fun encode(bytes: ByteArray, frameSize: Constants.FrameSize): ByteArray? {
        return encode(bytes, frameSize.v)
    }
    private external fun encode(bytes: ByteArray, frameSize: Int): ByteArray?

    fun encode(shorts: ShortArray, frameSize: Constants.FrameSize): ShortArray? {
        return encode(shorts, frameSize.v)
    }

    private external fun encode(shorts: ShortArray, frameSize: Int): ShortArray?
    external fun encoderRelease()

    //
    // Decoder
    //

    fun decoderCreate(
        sampleRate: Constants.SampleRate,
        channels: Constants.Channels
    ): Int {
        return decoderCreate(sampleRate.v, channels.v)
    }

    private external fun decoderCreate(sampleRate: Int, numChannels: Int): Int

    fun decoderInit(sampleRate: Constants.SampleRate, channels: Constants.Channels): Int {
        return decoderInit(sampleRate.v, channels.v)
    }

    private external fun decoderInit(sampleRate: Int, numChannels: Int): Int

    fun decode(bytes: ByteArray, frameSize: Constants.FrameSize): ByteArray? {
        return decode(bytes, frameSize.v)
    }

    private external fun decode(bytes: ByteArray, frameSize: Int): ByteArray?

    fun decode(shorts: ShortArray, frameSize: Constants.FrameSize): ShortArray? {
        return decode(shorts, frameSize.v)
    }
    private external fun decode(shorts: ShortArray, frameSize: Int): ShortArray?
    external fun decoderRelease()

    //
    // Utils
    //

    external fun convert(bytes: ByteArray): ShortArray?
    external fun convert(shorts: ShortArray): ByteArray?
}
