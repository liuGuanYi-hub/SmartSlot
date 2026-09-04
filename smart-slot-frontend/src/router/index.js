import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import MatrixView from '@/views/MatrixView.vue'
import MyBookingsView from '@/views/MyBookingsView.vue'
import LoginView from '@/views/LoginView.vue'
import AdminLayout from '@/views/admin/AdminLayout.vue'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import AdminVenues from '@/views/admin/AdminVenues.vue'
import AdminOrders from '@/views/admin/AdminOrders.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView
  },
  {
    path: '/matrix',
    name: 'Matrix',
    component: MatrixView
  },
  {
    path: '/my-bookings',
    name: 'MyBookings',
    component: MyBookingsView
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: AdminDashboard
      },
      {
        path: 'venues',
        name: 'AdminVenues',
        component: AdminVenues
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: AdminOrders
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
