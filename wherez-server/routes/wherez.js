exports.wherez = function (req, res) {
  const id = req.params.id || req.query.id || '';
  res.json({ id: id, msg: 'I am at Hawaii!!!' });
};
