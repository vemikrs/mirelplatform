
export default {
  mode: 'spa',
  /*
  ** Headers of the page
  */
  head: {
    title: process.env.npm_package_name || '',
    meta: [
      { charset: 'utf-8' },
      { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      { hid: 'description', name: 'description', content: process.env.npm_package_description || '' }
    ],
    link: [
      { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }
    ]
  },

  /** server settings */
  server: {
    port: 8081,
    host: '0.0.0.0'
  },

  /*
  ** Customize the progress-bar color
  */
  loading: { color: '#fff' },
  /*
  ** Global CSS
  */
  css: [
    '@/assets/styles/common.css'
  ],
  /*
  ** Plugins to load before mounting the App
  */
  plugins: [
    // axios.
    '~/plugins/src/axios'
  ],
  /*
  ** Nuxt.js dev-modules
  */
  buildModules: [
    // Typescript.
    '@nuxt/typescript-build',
    // Doc: https://github.com/nuxt-community/eslint-module
    '@nuxtjs/eslint-module'
  ],
  /*
  ** Nuxt.js modules
  */
  modules: [
    // Doc: https://bootstrap-vue.js.org
    'bootstrap-vue/nuxt',
    // Doc: https://axios.nuxtjs.org/usage
    '@nuxtjs/axios'
  ],
  /*
  ** Axios module configuration
  ** See https://axios.nuxtjs.org/options
  */
  axios: {
    proxy: true
  },
  /*
  ** Build configuration
  */
  build: {
    /*
    ** You can extend webpack config here
    */
    extend (config, ctx) {
    }
  },

  /*
   * append settings.
   */
  router: {
    base: '/mirel'
  },

  /*
   * proxy
   */
  proxy: {
    '/mste/initialize': 'http://zipcloud.ibsnet.co.jp/api/search?zipcode=7830060&limit=1',
    '/mapi/': {
      target: 'http://localhost:8080/mipla2/',
      pathRewrite: {
        '^/mapi/': '/'
      }
    },
    '/api/mste': {
      target: 'http://localhost:8080/mipla2/apps/mste/api/',
      pathRewrite: {
        '^/api/mste': '/'
      }
    }
  },

  /*
   * bootstrapVue
   */
  bootstrapVue: {
    /* bootstrapCSS */
    css: true,
    /* bootsrapVueCSS */
    bvCSS: true
  },

  configureWebpack: {
    devtool: 'source-map'
  }

}
