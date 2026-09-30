import os
import subprocess
import sys

def main():
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')

    root_dir = os.path.dirname(os.path.abspath(__file__))
    backend_dir = os.path.join(root_dir, "backend")
    
    # Check venv python
    venv_python = os.path.join(backend_dir, "venv", "Scripts", "python.exe")
    if not os.path.exists(venv_python):
        venv_python = sys.executable

    print("Iniciando el backend de BarboYa con FastAPI (Uvicorn)...")
    print("Servidor disponible en: http://127.0.0.1:8000")
    print("Documentacion API (Swagger): http://127.0.0.1:8000/api/v1/docs")
    print("-------------------------------------------------------")

    cmd = [venv_python, "-m", "uvicorn", "app.main:app", "--reload", "--host", "127.0.0.1", "--port", "8000"]
    try:
        subprocess.run(cmd, cwd=backend_dir)
    except KeyboardInterrupt:
        print("\nServidor detenido correctamente.")

if __name__ == "__main__":
    main()
