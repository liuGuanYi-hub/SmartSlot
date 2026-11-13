import request from './request'

// 获取指定拼场招募活动的微聊历史消息
export function getChatHistory(activityId) {
  return request({
    url: `/match/${activityId}/chat/history`,
    method: 'get'
  })
}

// 发送聊天消息 (普通文本或快捷战术短语)
export function sendChatMessage(data) {
  return request({
    url: `/match/${data.activityId}/chat/send`,
    method: 'post',
    data
  })
}
