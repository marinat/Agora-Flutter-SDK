package io.agora.rtc.base

import io.agora.rtc.IMetadataObserver
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtc.internal.EncryptionConfig
import java.util.*

class IRtcChannel {
  interface RtcChannelInterface : RtcAudioInterface, RtcVideoInterface, RtcVoicePositionInterface,
    RtcPublishStreamInterface, RtcMediaRelayInterface, RtcDualStreamInterface,
    RtcFallbackInterface, RtcMediaMetadataInterface, RtcEncryptionInterface,
    RtcInjectStreamInterface, RtcStreamMessageInterface {
    fun create(params: Map<String, *>, callback: Callback)

    fun destroy(params: Map<String, *>, callback: Callback)

    fun setClientRole(params: Map<String, *>, callback: Callback)

    fun joinChannel(params: Map<String, *>, callback: Callback)

    fun joinChannelWithUserAccount(params: Map<String, *>, callback: Callback)

    fun leaveChannel(params: Map<String, *>, callback: Callback)

    fun renewToken(params: Map<String, *>, callback: Callback)

    fun getConnectionState(params: Map<String, *>, callback: Callback)

    @Deprecated("")
    fun publish(params: Map<String, *>, callback: Callback)

    @Deprecated("")
    fun unpublish(params: Map<String, *>, callback: Callback)

    fun getCallId(params: Map<String, *>, callback: Callback)
  }

  interface RtcAudioInterface {
    fun adjustUserPlaybackSignalVolume(params: Map<String, *>, callback: Callback)

    fun muteLocalAudioStream(params: Map<String, *>, callback: Callback)

    fun muteRemoteAudioStream(params: Map<String, *>, callback: Callback)

    fun muteAllRemoteAudioStreams(params: Map<String, *>, callback: Callback)

    @Deprecated("")
    fun setDefaultMuteAllRemoteAudioStreams(params: Map<String, *>, callback: Callback)
  }

  interface RtcVideoInterface {
    fun muteLocalVideoStream(params: Map<String, *>, callback: Callback)

    fun muteRemoteVideoStream(params: Map<String, *>, callback: Callback)

    fun muteAllRemoteVideoStreams(params: Map<String, *>, callback: Callback)

    @Deprecated("")
    fun setDefaultMuteAllRemoteVideoStreams(params: Map<String, *>, callback: Callback)

    fun enableRemoteSuperResolution(params: Map<String, *>, callback: Callback)
  }

  interface RtcVoicePositionInterface {
    fun setRemoteVoicePosition(params: Map<String, *>, callback: Callback)
  }

  interface RtcPublishStreamInterface {
    fun setLiveTranscoding(params: Map<String, *>, callback: Callback)

    fun addPublishStreamUrl(params: Map<String, *>, callback: Callback)

    fun removePublishStreamUrl(params: Map<String, *>, callback: Callback)
  }

  interface RtcMediaRelayInterface {
    fun startChannelMediaRelay(params: Map<String, *>, callback: Callback)

    fun updateChannelMediaRelay(params: Map<String, *>, callback: Callback)

    fun stopChannelMediaRelay(params: Map<String, *>, callback: Callback)

    fun pauseAllChannelMediaRelay(params: Map<String, *>, callback: Callback)

    fun resumeAllChannelMediaRelay(params: Map<String, *>, callback: Callback)
  }

  interface RtcDualStreamInterface {
    fun setRemoteVideoStreamType(params: Map<String, *>, callback: Callback)

    fun setRemoteDefaultVideoStreamType(params: Map<String, *>, callback: Callback)
  }

  interface RtcFallbackInterface {
    fun setRemoteUserPriority(params: Map<String, *>, callback: Callback)
  }

  interface RtcMediaMetadataInterface {
    fun registerMediaMetadataObserver(params: Map<String, *>, callback: Callback)

    fun unregisterMediaMetadataObserver(params: Map<String, *>, callback: Callback)

    fun setMaxMetadataSize(params: Map<String, *>, callback: Callback)

    fun sendMetadata(params: Map<String, *>, callback: Callback)
  }

  interface RtcEncryptionInterface {
    @Deprecated("")
    fun setEncryptionSecret(params: Map<String, *>, callback: Callback)

    @Deprecated("")
    fun setEncryptionMode(params: Map<String, *>, callback: Callback)

    fun enableEncryption(params: Map<String, *>, callback: Callback)
  }

  interface RtcInjectStreamInterface {
    fun addInjectStreamUrl(params: Map<String, *>, callback: Callback)

    fun removeInjectStreamUrl(params: Map<String, *>, callback: Callback)
  }

  interface RtcStreamMessageInterface {
    fun createDataStream(params: Map<String, *>, callback: Callback)

    fun sendStreamMessage(params: Map<String, *>, callback: Callback)
  }
}

class RtcChannelManager(
  private val emit: (methodName: String, data: Map<String, Any?>?) -> Unit
) : IRtcChannel.RtcChannelInterface {
  private val rtcChannelMap = Collections.synchronizedMap(mutableMapOf<String, RtcChannel>())
  private val mediaObserverMap = Collections.synchronizedMap(mutableMapOf<String, MediaObserver>())

  fun release() {
    rtcChannelMap.forEach { it.value.destroy() }
    rtcChannelMap.clear()
    mediaObserverMap.clear()
  }

  operator fun get(channelId: String): RtcChannel? {
    return rtcChannelMap[channelId]
  }

  override fun create(params: Map<String, *>, callback: Callback) {
    callback.resolve(params["engine"] as RtcEngine) { e ->
      e.createRtcChannel(params["channelId"] as String)?.let {
        it.setRtcChannelEventHandler(RtcChannelEventHandler { methodName, data ->
          emit(
            methodName,
            data
          )
        })
        rtcChannelMap[it.channelId()] = it
      }
      Unit
    }
  }

  override fun destroy(params: Map<String, *>, callback: Callback) {
    callback.code(rtcChannelMap.remove(params["channelId"] as String)?.destroy())
  }

  override fun setClientRole(params: Map<String, *>, callback: Callback) {
    val role = (params["role"] as Number).toInt()
    (params["options"] as? Map<*, *>)?.let {
      callback.code(
        this[params["channelId"] as String]?.setClientRole(
          role,
          mapToClientRoleOptions(it)
        )
      )
      return@setClientRole
    }
    callback.code(this[params["channelId"] as String]?.setClientRole(role))
  }

  override fun joinChannel(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.joinChannel(
        params["token"] as? String,
        params["optionalInfo"] as? String,
        (params["optionalUid"] as Number).toNativeUInt(),
        mapToChannelMediaOptions(params["options"] as Map<*, *>)
      )
    )
  }

  override fun joinChannelWithUserAccount(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.joinChannelWithUserAccount(
        params["token"] as? String,
        params["userAccount"] as String,
        mapToChannelMediaOptions(params["options"] as Map<*, *>)
      )
    )
  }

  override fun leaveChannel(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.leaveChannel())
  }

  override fun renewToken(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.renewToken(params["token"] as String))
  }

  override fun getConnectionState(params: Map<String, *>, callback: Callback) {
    callback.resolve(this[params["channelId"] as String]) { it.connectionState }
  }

  override fun publish(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.publish())
  }

  override fun unpublish(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.unpublish())
  }

  override fun getCallId(params: Map<String, *>, callback: Callback) {
    callback.resolve(this[params["channelId"] as String]) { it.callId }
  }

  override fun adjustUserPlaybackSignalVolume(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.adjustUserPlaybackSignalVolume(
        (params["uid"] as Number).toNativeUInt(),
        (params["volume"] as Number).toInt()
      )
    )
  }

  override fun muteLocalAudioStream(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.muteLocalAudioStream(
        params["muted"] as Boolean
      )
    )
  }

  override fun muteRemoteAudioStream(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.muteRemoteAudioStream(
        (params["uid"] as Number).toNativeUInt(),
        params["muted"] as Boolean
      )
    )
  }

  override fun muteAllRemoteAudioStreams(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.muteAllRemoteAudioStreams(params["muted"] as Boolean))
  }

  override fun setDefaultMuteAllRemoteAudioStreams(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.setDefaultMuteAllRemoteAudioStreams(params["muted"] as Boolean))
  }

  override fun muteLocalVideoStream(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.muteLocalVideoStream(
        params["muted"] as Boolean
      )
    )
  }

  override fun muteRemoteVideoStream(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.muteRemoteVideoStream(
        (params["uid"] as Number).toNativeUInt(),
        params["muted"] as Boolean
      )
    )
  }

  override fun muteAllRemoteVideoStreams(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.muteAllRemoteVideoStreams(params["muted"] as Boolean))
  }

  override fun setDefaultMuteAllRemoteVideoStreams(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.setDefaultMuteAllRemoteVideoStreams(params["muted"] as Boolean))
  }

  override fun enableRemoteSuperResolution(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.enableRemoteSuperResolution(
        (params["uid"] as Number).toNativeUInt(),
        params["enable"] as Boolean
      )
    )
  }

  override fun setRemoteVoicePosition(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.setRemoteVoicePosition(
        (params["uid"] as Number).toNativeUInt(),
        (params["pan"] as Number).toDouble(),
        (params["gain"] as Number).toDouble()
      )
    )
  }

  override fun setLiveTranscoding(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.setLiveTranscoding(
        mapToLiveTranscoding(
          params["transcoding"] as Map<*, *>
        )
      )
    )
  }

  override fun addPublishStreamUrl(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.addPublishStreamUrl(
        params["url"] as String,
        params["transcodingEnabled"] as Boolean
      )
    )
  }

  override fun removePublishStreamUrl(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.removePublishStreamUrl(params["url"] as String))
  }

  override fun startChannelMediaRelay(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.startChannelMediaRelay(
        mapToChannelMediaRelayConfiguration(params["channelMediaRelayConfiguration"] as Map<*, *>)
      )
    )
  }

  override fun updateChannelMediaRelay(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.updateChannelMediaRelay(
        mapToChannelMediaRelayConfiguration(params["channelMediaRelayConfiguration"] as Map<*, *>)
      )
    )
  }

  override fun stopChannelMediaRelay(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.stopChannelMediaRelay())
  }

  override fun pauseAllChannelMediaRelay(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.pauseAllChannelMediaRelay())
  }

  override fun resumeAllChannelMediaRelay(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.resumeAllChannelMediaRelay())
  }

  override fun setRemoteVideoStreamType(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.setRemoteVideoStreamType(
        (params["uid"] as Number).toNativeUInt(),
        (params["streamType"] as Number).toInt()
      )
    )
  }

  override fun setRemoteDefaultVideoStreamType(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.setRemoteDefaultVideoStreamType((params["streamType"] as Number).toInt()))
  }

  override fun setRemoteUserPriority(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.setRemoteUserPriority(
        (params["uid"] as Number).toNativeUInt(),
        (params["userPriority"] as Number).toInt()
      )
    )
  }

  override fun registerMediaMetadataObserver(params: Map<String, *>, callback: Callback) {
    val channelId = params["channelId"] as String
    val mediaObserver = MediaObserver { data ->
      emit(
        RtcChannelEvents.MetadataReceived,
        data?.toMutableMap()?.apply { put("channelId", channelId) })
    }
    callback.code(
      this[channelId]?.registerMediaMetadataObserver(
        mediaObserver,
        IMetadataObserver.VIDEO_METADATA
      )
    ) {
      mediaObserverMap[channelId] = mediaObserver
      Unit
    }
  }

  override fun unregisterMediaMetadataObserver(params: Map<String, *>, callback: Callback) {
    val channelId = params["channelId"] as String
    callback.code(
      this[channelId]?.registerMediaMetadataObserver(
        null,
        IMetadataObserver.VIDEO_METADATA
      )
    ) {
      mediaObserverMap.remove(channelId)
      Unit
    }
  }

  override fun setMaxMetadataSize(params: Map<String, *>, callback: Callback) {
    callback.resolve(mediaObserverMap[params["channelId"] as String]) {
      it.maxMetadataSize = (params["size"] as Number).toInt()
      Unit
    }
  }

  override fun sendMetadata(params: Map<String, *>, callback: Callback) {
    callback.resolve(mediaObserverMap[params["channelId"] as String]) {
      it.addMetadata(params["metadata"] as String)
      Unit
    }
  }

  override fun setEncryptionSecret(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.setEncryptionSecret(params["secret"] as String))
  }

  override fun setEncryptionMode(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.setEncryptionMode(
        when ((params["encryptionMode"] as Number).toInt()) {
          EncryptionConfig.EncryptionMode.AES_128_XTS.value -> "aes-128-xts"
          EncryptionConfig.EncryptionMode.AES_128_ECB.value -> "aes-128-ecb"
          EncryptionConfig.EncryptionMode.AES_256_XTS.value -> "aes-256-xts"
          else -> ""
        }
      )
    )
  }

  override fun enableEncryption(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.enableEncryption(
        params["enabled"] as Boolean,
        mapToEncryptionConfig(params["config"] as Map<*, *>)
      )
    )
  }

  override fun addInjectStreamUrl(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.addInjectStreamUrl(
        params["url"] as String,
        mapToLiveInjectStreamConfig(params["config"] as Map<*, *>)
      )
    )
  }

  override fun removeInjectStreamUrl(params: Map<String, *>, callback: Callback) {
    callback.code(this[params["channelId"] as String]?.removeInjectStreamUrl(params["url"] as String))
  }

  override fun createDataStream(params: Map<String, *>, callback: Callback) {
    val channel = this[params["channelId"] as String]
    (params["config"] as? Map<*, *>)?.let { config ->
      callback.code(channel?.createDataStream(mapToDataStreamConfig(config))) { it }
      return@createDataStream
    }
    callback.code(
      channel?.createDataStream(
        params["reliable"] as Boolean,
        params["ordered"] as Boolean
      )
    ) { it }
  }

  override fun sendStreamMessage(params: Map<String, *>, callback: Callback) {
    callback.code(
      this[params["channelId"] as String]?.sendStreamMessage(
        (params["streamId"] as Number).toInt(),
        (params["message"] as String).toByteArray()
      )
    )
  }
}
