import axios from 'axios'
import { apiBaseUrl } from './apiConfig'

export const httpClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
})
