import request from './request'

export function getCategories() {
  return request({
    url: '/venues/categories',
    method: 'get'
  })
}

export function getVenues(params) {
  return request({
    url: '/venues',
    method: 'get',
    params
  })
}

// 获取核心日历时段矩阵看板数据
export function getSlotMatrix(params) {
  return request({
    url: '/venues/matrix',
    method: 'get',
    params
  })
}

export function getVenueDetail(id) {
  return request({
    url: `/venues/${id}`,
    method: 'get'
  })
}

export function getVenueReviews(id) {
  return request({
    url: `/venues/${id}/reviews`,
    method: 'get'
  })
}

// 管理端 API
export function getAdminVenuesPage(params) {
  return request({
    url: '/admin/venues/page',
    method: 'get',
    params
  })
}

export function saveVenue(data) {
  return request({
    url: '/admin/venues',
    method: 'post',
    data
  })
}

export function deleteVenue(id) {
  return request({
    url: `/admin/venues/${id}`,
    method: 'delete'
  })
}
