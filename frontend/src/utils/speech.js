const MUTE_KEY = 'med-reminder:voice-muted'

let muted = localStorage.getItem(MUTE_KEY) === '1'

export function isMuted() {
  return muted
}

export function setMuted(value) {
  muted = value
  localStorage.setItem(MUTE_KEY, value ? '1' : '0')
}

/** 浏览器自带语音播报，无需联网、无需密钥 */
export function speak(text) {
  if (muted || !text) {
    return
  }
  if (!('speechSynthesis' in window)) {
    console.warn('当前浏览器不支持语音播报')
    return
  }
  try {
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    utterance.rate = 0.9
    window.speechSynthesis.speak(utterance)
  } catch (e) {
    console.warn('语音播报失败', e)
  }
}

