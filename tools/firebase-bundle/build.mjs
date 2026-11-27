import { build } from 'esbuild';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const here = path.dirname(fileURLToPath(import.meta.url));
await build({
  entryPoints: [path.join(here, 'entry.js')],
  outfile: path.join(here, '..', '..', 'assets', 'firebase-bundle.js'),
  bundle: true, format: 'esm', minify: true, target: 'es2020', legalComments: 'eof', logLevel: 'info'
});
