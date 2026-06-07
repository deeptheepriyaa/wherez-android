exports.index = function (req, res) {
  const id = req.query.id || req.params.id || '';
  res.json({ id: id, msg: 'I am at Hawaii!!!' });
};
