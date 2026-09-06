import request from './request'

// ==========================================
// 1. 用户个人中心与钱包 API
// ==========================================

export function getUserProfile() {
  return request({
    url: '/user/profile',
    method: 'get'
  })
}

export function updateUserProfile(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

export function updateUserPassword(data) {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

export function rechargeWallet(data) {
  return request({
    url: '/user/wallet/recharge',
    method: 'post',
    data
  })
}

export function getMyWalletRecords() {
  return request({
    url: '/user/wallet/records',
    method: 'get'
  })
}

// ==========================================
// 2. 管理后台会员中台 API
// ==========================================

export function getAdminUsersPage(params) {
  return request({
    url: '/admin/users/page',
    method: 'get',
    params
  })
}

export function updateAdminUserStatus(id, status) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}

export function updateAdminUserRole(id, role) {
  return request({
    url: `/admin/users/${id}/role`,
    method: 'put',
    data: { role }
  })
}

export function adjustAdminUserBalance(id, data) {
  return request({
    url: `/admin/users/${id}/balance`,
    method: 'post',
    data
  })
}

export function adjustAdminUserCredit(id, creditScore) {
  return request({
    url: `/admin/users/${id}/credit`,
    method: 'put',
    data: { creditScore }
  })
}
