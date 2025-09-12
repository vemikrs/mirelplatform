export default function ({ $axios, redirect }) {
  $axios.setToken('access_token')

  $axios.onResponse(() => {
    $axios.setHeader('Access-Control-Allow-Origin', '*')
  })
}
