const webpack = require('webpack')

module.exports = {
  productionSourceMap: false,

  configureWebpack: {
    plugins: [
      new webpack.DefinePlugin({
        '__VUE_OPTIONS_API__': false,
        '__VUE_PROD_DEVTOOLS__': false
      })
    ]
  },
  css: {
    loaderOptions: {
      css: {
        localsConvention: 'camelCaseOnly'
      }
    }
  },

  publicPath: '',
  assetsDir: 'static'
}
