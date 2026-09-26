// The first Compose scene in headless Chrome can take longer than the two seconds mocha allows by
// default, so each test gets thirty.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 30000 });
