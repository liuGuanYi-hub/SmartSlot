import request from './request'

// 发起拼场招募
export function createMatch(data) {
  return request({
    url: '/match/create',
    method: 'post',
    data
  })
}

// 拼场大厅分页列表
export function getMatchPage(params) {
  return request({
    url: '/match/page',
    method: 'get',
    params
  })
}

// 获取拼场招募详情
export function getMatchDetail(id) {
  return request({
    url: `/match/${id}`,
    method: 'get'
  })
}

// 球友一键上车加入拼场
export function joinMatch(id) {
  return request({
    url: `/match/${id}/join`,
    method: 'post'
  })
}

// 发起人取消/解散拼场
export function cancelMatch(id) {
  return request({
    url: `/match/${id}/cancel`,
    method: 'post'
  })
}

// 查询我的拼场列表 (发起与加入)
export function getMyMatches() {
  return request({
    url: '/match/my',
    method: 'get'
  })
}
