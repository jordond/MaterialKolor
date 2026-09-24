// Production names every script and wasm file by its content, and `assembleSite` moves them under
// /assets/ where the host caches them for a year. The page loads them from there whatever path it
// was opened on, /t/<code> included.
//
// Development and both dev servers keep plain names at the root, so the server and the source
// index.html agree. Only run tasks set devServer, and it is set before this file runs.
if (config.mode === 'production' && !config.devServer) {
  config.output = config.output || {};
  config.output.publicPath = '/assets/';
  config.output.filename = (pathData) =>
    pathData.chunk.name === 'main' ? 'builder.[contenthash:16].js' : '[name].[contenthash:16].js';
  config.output.chunkFilename = '[name].[contenthash:16].js';
  config.output.assetModuleFilename = '[name].[contenthash:16][ext]';
}
