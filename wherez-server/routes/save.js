const { MongoClient } = require('mongodb');

const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017';
const DB_NAME   = 'wherezDb';
const COL_NAME  = 'locations';

exports.save = async function (req, res) {
  const data = Object.assign({}, req.query, req.body, req.params);
  try {
    const client = new MongoClient(MONGO_URI);
    await client.connect();
    const col = client.db(DB_NAME).collection(COL_NAME);
    await col.insertOne({ ...data, savedAt: new Date() });
    await client.close();
    res.json({ ok: true, saved: data });
  } catch (err) {
    console.error('MongoDB error:', err);
    res.status(500).json({ ok: false, error: err.message });
  }
};
