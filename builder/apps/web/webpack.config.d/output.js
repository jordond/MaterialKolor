// Production names every script and wasm file by its content, and `assembleSite` moves them under
// /assets/ where the host caches them for a year. The page loads them from there whatever path it
// was opened on, /t/<code> included. The main chunk keeps the name the build script gives it,
// builder for wasm and builder-js for JS, so both glues can sit side by side in /assets/.
//
// Development and both dev servers keep plain names at the root, so the server and the source
// index.html agree. Only run tasks set devServer, and it is set before this file runs.
if (config.mode === 'production' && !config.devServer) {
  config.output = config.output || {};
  const configured = config.output.filename;
  const main = typeof configured === 'function' ? configured({ chunk: { name: 'main' } }) : configured;
  if (typeof main !== 'string' || !/\.js$/.test(main)) {
    throw new Error('output.js expected the main chunk to be named <name>.js, found ' + main);
  }
  const glue = main.replace(/\.js$/, '.[contenthash:16].js');
  config.output.publicPath = '/assets/';
  config.output.filename = (pathData) => (pathData.chunk.name === 'main' ? glue : '[name].[contenthash:16].js');
  config.output.chunkFilename = '[name].[contenthash:16].js';
  config.output.assetModuleFilename = '[name].[contenthash:16][ext]';
}
