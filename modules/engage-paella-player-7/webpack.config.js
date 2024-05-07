/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const webpack = require('webpack');



module.exports = function (env) {
  const ocServer = env.server || 'http://localhost:8080';

  return {
    entry: './src/index.js',
    output: {
      path: path.join(__dirname,'target/paella-build'),
      filename: 'paella-player.js',
      publicPath: env.PUBLIC_PATH ?? '/paella7/ui'
    },
    devtool: 'source-map',
    devServer: {
      port: 7070,
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, PATCH, OPTIONS',
        'Access-Control-Allow-Headers': 'X-Requested-With, content-type, Authorization'
      },
      static: [
        {
          directory: path.join(__dirname, '../../etc/ui-config/mh_default_org/paella7'),
          publicPath: env.OPENCAST_CONFIG_URL ?? '/ui/config/paella7'
        },
        {
          directory: path.join(__dirname, 'tests/mock/static'),
          publicPath: '/test_mock_static'
        }
      ],
      proxy: [
        {
          context: [
            '/search/**',
            '/info/**',
            '/series/**',
            '/annotation/**',
            '/engage/**',
            '/play/**',
            '/usertracking/**',
            '/editor/**',
            '/editor-ui/**'
          ],
          target: ocServer,
          secure: false,
          changeOrigin: true
        },
      ]
    },

    module: {
      rules: [
        {
          test: /\.js$/,
          exclude: /(node_modules)/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/preset-env']
            }
          }
        },

        {
          test: /\.js$/,
          enforce: 'pre',
          use: ['source-map-loader']
        },

        {
          test: /\.svg$/i,
          use: {
            loader: 'svg-inline-loader'
          }
        },
        {
          test: /\.css$/i,
          use: ['style-loader', 'css-loader']
        },
        {
          test: /\.xml$/,
          use: {
            loader: 'xml-loader',
            options: {
              explicitArray: false,
            },
          },
        }
      ]
    },

    plugins: [
      new webpack.DefinePlugin({
        OPENCAST_SERVER_URL: JSON.stringify(env.OPENCAST_SERVER_URL),
        OPENCAST_CONFIG_URL: JSON.stringify(env.OPENCAST_CONFIG_URL),
        OPENCAST_PAELLA_URL: JSON.stringify(env.PUBLIC_PATH)
      }),
      new webpack.SourceMapDevToolPlugin({
        filename: '[file].js.map[query]'
      }),
      new CopyWebpackPlugin({
        patterns: [
          { from: 'public', to: '' },
          { from: './node_modules/paella-skins/skins/opencast', to: 'default_theme' }
        ]
      })
    ],
    performance: {
      hints: false,
      maxEntrypointSize: 4194304,
      maxAssetSize: 4194304
    }
  }
};
