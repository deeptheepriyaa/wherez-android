const express = require('express');
const routes  = require('./routes');
const wherez  = require('./routes/wherez');
const save    = require('./routes/save');

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.set('port', process.env.PORT || 3001);

app.get('/',            routes.index);
app.get('/wherez/:id',  wherez.wherez);
app.post('/wherez/:id', wherez.wherez);
app.get('/save/:id',    save.save);
app.post('/save/:id',   save.save);

app.listen(app.get('port'), '0.0.0.0', () => {
  console.log('wherez server listening on port %d', app.get('port'));
});
