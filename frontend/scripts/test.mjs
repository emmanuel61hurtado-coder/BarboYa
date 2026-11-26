// Vitest 4 falla en Windows si la ruta llega con la unidad en minúscula (c:\...),
// como ocurre en la terminal de VS Code. Se normaliza antes de arrancarlo.
import { spawnSync } from 'node:child_process'
import { realpathSync } from 'node:fs'
import { join } from 'node:path'

const cwd = realpathSync.native(process.cwd())
const vitest = join(cwd, 'node_modules', 'vitest', 'vitest.mjs')
const result = spawnSync(process.execPath, [vitest, 'run', ...process.argv.slice(2)], { cwd, stdio: 'inherit' })

process.exit(result.status ?? 1)
