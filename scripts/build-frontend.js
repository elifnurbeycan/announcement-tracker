const fs = require('fs');
const path = require('path');
const Babel = require('../src/main/resources/static/vendor/babel.min.js');

const projectRoot = path.resolve(__dirname, '..');
const staticRoot = path.join(projectRoot, 'src', 'main', 'resources', 'static');
const frontendRoot = path.join(projectRoot, 'src', 'main', 'frontend');
const distRoot = path.join(staticRoot, 'js', 'dist');

const jsxFiles = [
  'components/Toast.js',
  'components/Sidebar.js',
  'components/Header.js',
  'components/Modals.js',
  'pages/OverviewPage.js',
  'pages/AnnouncementsPage.js',
  'pages/SubscribersPage.js',
  'pages/DepartmentsPage.js',
  'pages/SourcesPage.js',
  'pages/SettingsPage.js',
  'pages/ProfilePage.js',
  'app.js',
  'userApp.js'
];

const loginPages = [
  { html: 'admin-login.html', source: 'admin-login.jsx', output: 'admin-login.js' },
  { html: 'user-login.html', source: 'user-login.jsx', output: 'user-login.js' }
];

function ensureParent(file) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
}

function compile(source, filename) {
  return Babel.transform(source, {
    presets: ['react'],
    sourceType: 'script',
    filename,
    comments: false,
    compact: false
  }).code + '\n';
}

for (const relativeFile of jsxFiles) {
  const sourceFile = path.join(staticRoot, 'js', relativeFile);
  const outputFile = path.join(distRoot, relativeFile);
  ensureParent(outputFile);
  fs.writeFileSync(outputFile, compile(fs.readFileSync(sourceFile, 'utf8'), relativeFile), 'utf8');
}

for (const page of loginPages) {
  const htmlFile = path.join(staticRoot, page.html);
  const sourceFile = path.join(frontendRoot, page.source);
  let html = fs.readFileSync(htmlFile, 'utf8');

  const inlineBabel = /\s*<script type="text\/babel">([\s\S]*?)<\/script>\s*<\/body>/;
  const match = html.match(inlineBabel);
  if (match) {
    ensureParent(sourceFile);
    fs.writeFileSync(sourceFile, match[1].trim() + '\n', 'utf8');
    html = html.replace(inlineBabel, `\n    <script src="/js/dist/${page.output}"></script>\n</body>`);
  }

  html = html.replace(/\s*<script src="\/vendor\/babel\.min\.js"><\/script>/, '');
  fs.writeFileSync(htmlFile, html, 'utf8');

  const outputFile = path.join(distRoot, page.output);
  ensureParent(outputFile);
  fs.writeFileSync(outputFile, compile(fs.readFileSync(sourceFile, 'utf8'), page.source), 'utf8');
}

for (const htmlName of ['dashboard.html', 'user-dashboard.html']) {
  const htmlFile = path.join(staticRoot, htmlName);
  let html = fs.readFileSync(htmlFile, 'utf8');
  html = html.replace(/\s*<script src="\/vendor\/babel\.min\.js"><\/script>/, '');
  html = html.replace(/<script type="text\/babel" src="\/js\/([^"]+)"><\/script>/g,
    '<script src="/js/dist/$1"></script>');
  fs.writeFileSync(htmlFile, html, 'utf8');
}

const indexFile = path.join(staticRoot, 'index.html');
let indexHtml = fs.readFileSync(indexFile, 'utf8');
indexHtml = indexHtml.replace('https://unpkg.com/lucide@latest', '/vendor/lucide.min.js');
fs.writeFileSync(indexFile, indexHtml, 'utf8');

console.log(`Frontend compiled to ${path.relative(projectRoot, distRoot)}`);
