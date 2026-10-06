/**
 * 语音识别：浏览器录音 -> 转成 16k 单声道 WAV -> 交给后端调用百度语音识别。
 *
 * 为什么不直接用浏览器自带的 SpeechRecognition：
 * 它依赖谷歌服务器，国内网络基本连不上（报 network 错误）。
 */

let mediaRecorder = null
let mediaStream = null
let chunks = []

export function isRecording() {
  return !!mediaRecorder && mediaRecorder.state === 'recording'
}

export async function startRecording(onError) {
  if (!window.isSecureContext) {
    onError?.('麦克风需要 https 或 localhost 才能使用')
    return false
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    onError?.('当前浏览器不支持录音功能')
    return false
  }
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
  } catch (e) {
    onError?.('没有拿到麦克风权限，请在地址栏允许后重试')
    return false
  }
  chunks = []
  mediaRecorder = new MediaRecorder(mediaStream)
  mediaRecorder.ondataavailable = (event) => {
    if (event.data && event.data.size > 0) {
      chunks.push(event.data)
    }
  }
  mediaRecorder.start()
  return true
}

/** 停止录音并返回 base64 的 WAV 数据；没有录到声音返回 null。 */
export function stopRecording() {
  return new Promise((resolve) => {
    if (!mediaRecorder) {
      resolve(null)
      return
    }
    mediaRecorder.onstop = async () => {
      mediaStream?.getTracks().forEach((track) => track.stop())
      mediaRecorder = null
      try {
        const blob = new Blob(chunks, { type: chunks[0]?.type || 'audio/webm' })
        const base64 = await blobToWavBase64(blob)
        resolve(base64)
      } catch (e) {
        console.warn('音频转换失败', e)
        resolve(null)
      }
    }
    mediaRecorder.stop()
  })
}

/** 把录音 Blob 解码后重采样为 16k 单声道 WAV（百度要求的格式），并转 base64。 */
async function blobToWavBase64(blob) {
  const arrayBuffer = await blob.arrayBuffer()
  const AudioCtx = window.AudioContext || window.webkitAudioContext
  const ctx = new AudioCtx()
  const audioBuffer = await ctx.decodeAudioData(arrayBuffer)

  const targetRate = 16000
  const ratio = audioBuffer.sampleRate / targetRate
  const length = Math.max(1, Math.floor(audioBuffer.length / ratio))
  const channels = []
  for (let c = 0; c < audioBuffer.numberOfChannels; c++) {
    channels.push(audioBuffer.getChannelData(c))
  }
  const samples = new Float32Array(length)
  for (let i = 0; i < length; i++) {
    const index = Math.floor(i * ratio)
    let sum = 0
    for (let c = 0; c < channels.length; c++) {
      sum += channels[c][index] || 0
    }
    samples[i] = sum / channels.length
  }
  await ctx.close().catch(() => {})

  const buffer = new ArrayBuffer(44 + samples.length * 2)
  const view = new DataView(buffer)
  const writeText = (offset, text) => {
    for (let i = 0; i < text.length; i++) {
      view.setUint8(offset + i, text.charCodeAt(i))
    }
  }
  writeText(0, 'RIFF')
  view.setUint32(4, 36 + samples.length * 2, true)
  writeText(8, 'WAVE')
  writeText(12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, targetRate, true)
  view.setUint32(28, targetRate * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeText(36, 'data')
  view.setUint32(40, samples.length * 2, true)
  let offset = 44
  for (let i = 0; i < samples.length; i++) {
    const clamped = Math.max(-1, Math.min(1, samples[i]))
    view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true)
    offset += 2
  }

  let binary = ''
  const bytes = new Uint8Array(buffer)
  const step = 0x8000
  for (let i = 0; i < bytes.length; i += step) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + step))
  }
  return btoa(binary)
}

