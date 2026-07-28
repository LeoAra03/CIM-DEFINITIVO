import socket
import threading
import tkinter as tk
from tkinter import ttk, messagebox

class TCPServerApp:
    """
    Aplicación de servidor TCP con interfaz gráfica Tkinter.
    Gestiona conexiones de clientes en hilos separados y permite enviar comandos
    a los clientes conectados.
    """
    def __init__(self, master):
        self.master = master
        self.master.title("Servidor Coordinador")
        self.master.geometry("600x600")
        self.master.resizable(False, False)

        # --- Configuración de Red ---
        self.HOST = '0.0.0.0'
        self.PORT = 8888
        self.MAX_CONNECTIONS = 2

        # --- Estado del Servidor ---
        self.server_socket = None
        self.connections = []
        self.server_running = False
        self.connections_lock = threading.Lock()

        self.create_widgets()
        self.master.protocol("WM_DELETE_WINDOW", self.on_closing)

    def create_widgets(self):
        """Crea todos los widgets de la interfaz gráfica."""
        # Frames
        frame_chatservidor = tk.LabelFrame(self.master, text="Chat servidor", font=("Arial", 10, "bold"))
        frame_chatservidor.place(x=5, y=5, width=415, height=300)

        frame_plc = tk.LabelFrame(self.master, text="PLC", font=("Arial", 10, "bold"))
        frame_plc.place(x=5, y=310, width=190, height=100)

        frame_almacen = tk.LabelFrame(self.master, text="Almacen", font=("Arial", 10, "bold"))
        frame_almacen.place(x=200, y=310, width=220, height=100)

        frame_laser = tk.LabelFrame(self.master, text="Laser", font=("Arial", 10, "bold"))
        frame_laser.place(x=5, y=415, width=210, height=100)

        # --- Widgets del Panel de Servidor ---
        self.log_text = tk.Text(frame_chatservidor)
        self.log_text.place(x=5, y=10, width=398, height=190)

        self.estado_label = tk.Label(frame_chatservidor, text='Servidor detenido', bd=1, relief="solid", bg="red")
        self.estado_label.place(x=50, y=210)

        self.mensaje_text = tk.Text(frame_chatservidor, state=tk.DISABLED)
        self.mensaje_text.place(x=5, y=240, width=400, height=20)
        self.mensaje_text.bind("<Return>", self.enviar_mensaje)

        self.start_button = tk.Button(frame_chatservidor, text='Iniciar', width=9, command=self.iniciar_servidor)
        self.start_button.place(x=160, y=210)

        self.stop_button = tk.Button(frame_chatservidor, text='Detener', width=9, command=self.detener_servidor, state=tk.DISABLED)
        self.stop_button.place(x=240, y=210)

        # --- Widgets del Panel PLC ---
        tk.Label(frame_plc, text="Pallet").place(x=10, y=10)
        self.pallet_combo = ttk.Combobox(frame_plc, state="readonly", values=[1, 2, 3, 5, 6], width=5)
        self.pallet_combo.set(1)
        self.pallet_combo.place(x=70, y=10)

        tk.Label(frame_plc, text="Estación").place(x=10, y=40)
        self.estacion_combo = ttk.Combobox(frame_plc, state="readonly", values=[1, 2, 3, 5, 6], width=5)
        self.estacion_combo.set(1)
        self.estacion_combo.place(x=70, y=40)

        self.b_deliver = tk.Button(frame_plc, text="Deliver", command=lambda: self.enviar_comando("PLC", [self.estacion_combo.get(), self.pallet_combo.get(), "deliver"]), state=tk.DISABLED)
        self.b_deliver.place(x=130, y=10)

        self.b_free = tk.Button(frame_plc, text="Free", command=lambda: self.enviar_comando("PLC", [self.estacion_combo.get(), self.pallet_combo.get(), "free"]), state=tk.DISABLED)
        self.b_free.place(x=130, y=40)

        # --- Widgets del Panel Almacén ---
        tk.Label(frame_almacen, text="Nº ArUco").place(x=10, y=10)
        self.spin_aruco_almacen = tk.Spinbox(frame_almacen, from_=1, to=15, font=("Arial", 12), width=2)
        self.spin_aruco_almacen.place(x=80, y=10)

        tk.Label(frame_almacen, text="Posición").place(x=10, y=40)
        self.spin_posicion = tk.Spinbox(frame_almacen, from_=1, to=3, font=("Arial", 12), width=2)
        self.spin_posicion.place(x=80, y=40)

        self.b_almacenar = tk.Button(frame_almacen, text="Almacenar", command=lambda: self.enviar_comando("Almacen", [self.spin_posicion.get(), self.spin_aruco_almacen.get(), "Almacenar"]), state=tk.DISABLED)
        self.b_almacenar.place(x=130, y=10)

        self.b_retirar = tk.Button(frame_almacen, text="Retirar", command=lambda: self.enviar_comando("Almacen", [self.spin_posicion.get(), self.spin_aruco_almacen.get(), "Retirar"]), state=tk.DISABLED)
        self.b_retirar.place(x=130, y=40)

        # --- Widgets del Panel Láser ---
        tk.Label(frame_laser, text="Nº ArUco").place(x=10, y=10)
        self.spin_aruco_laser = tk.Spinbox(frame_laser, from_=1, to=15, font=("Arial", 12), width=2)
        self.spin_aruco_laser.place(x=80, y=10)

        self.b_imprimir = tk.Button(frame_laser, text="Imprimir", command=lambda: self.enviar_comando("Laser,Imprimir", [self.spin_aruco_laser.get()]), state=tk.DISABLED)
        self.b_imprimir.place(x=130, y=6)

        self.b_posicionar = tk.Button(frame_laser, text="Posicionar", command=lambda: self.enviar_comando("Laser", ["Posicionar"]), state=tk.DISABLED)
        self.b_posicionar.place(x=10, y=40)

        self.b_retirar_laser = tk.Button(frame_laser, text="Retirar", command=lambda: self.enviar_comando("Laser", ["Retirar"]), state=tk.DISABLED)
        self.b_retirar_laser.place(x=80, y=40)

        self.command_buttons = [
            self.b_deliver, self.b_free, self.b_imprimir, self.b_almacenar,
            self.b_retirar, self.b_posicionar, self.b_retirar_laser
        ]

    def update_ui_state(self, is_running):
        """Actualiza el estado de los widgets de la UI."""
        server_state = tk.NORMAL if is_running else tk.DISABLED
        button_state = tk.DISABLED if is_running else tk.NORMAL

        self.start_button.config(state=button_state)
        self.stop_button.config(state=server_state)
        self.mensaje_text.config(state=server_state)

        for btn in self.command_buttons:
            btn.config(state=server_state)

        if is_running:
            self.estado_label.config(text='Servidor corriendo', bg="lightgreen")
        else:
            self.estado_label.config(text='Servidor Detenido', bg="red")

    def log(self, mensaje):
        """Muestra mensajes en el área de log de la interfaz."""
        self.log_text.insert(tk.END, mensaje + '\n')
        self.log_text.see(tk.END)

    def iniciar_servidor(self):
        """Inicia el hilo del servidor."""
        servidor_thread = threading.Thread(target=self.correr_servidor, daemon=True)
        servidor_thread.start()
        self.update_ui_state(True)

    def detener_servidor(self):
        """Detiene el servidor y cierra todas las conexiones."""
        self.server_running = False
        with self.connections_lock:
            for conn in self.connections:
                try:
                    conn.close()
                except Exception:
                    pass
            self.connections.clear()

        try:
            if self.server_socket:
                self.server_socket.close()
        except Exception:
            pass

        self.update_ui_state(False)
        self.log("Servidor detenido y sockets cerrados.")

    def correr_servidor(self):
        """Bucle principal del servidor que acepta conexiones."""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        try:
            self.server_socket.bind((self.HOST, self.PORT))
            self.server_socket.listen(self.MAX_CONNECTIONS)
            self.log(f'Servidor corriendo en el puerto {self.PORT}')
        except OSError as e:
            self.log(f'Error al iniciar el servidor: {e}')
            self.master.after(0, self.update_ui_state, False)
            return

        self.server_running = True
        while self.server_running:
            try:
                self.server_socket.settimeout(1.0)
                conn, addr = self.server_socket.accept()
                
                with self.connections_lock:
                    self.connections.append(conn)

                thread = threading.Thread(target=self.manejar_conexion, args=(conn, addr), daemon=True)
                thread.start()
            except socket.timeout:
                continue
            except OSError:
                break # Socket cerrado

    def manejar_conexion(self, conn, addr):
        """Maneja la conexión de un cliente individual."""
        name = f"{addr[0]}:{addr[1]}"
        self.log(f'{name} conectado.')

        try:
            while self.server_running:
                data = conn.recv(1024)
                if not data:
                    break
                mensaje = data.decode().strip()
                self.log(f'Datos recibidos de {name}: {mensaje}')
                conn.sendall(f'Recibido: {mensaje}'.encode())
        except (ConnectionResetError, BrokenPipeError):
            self.log(f'Cliente {name} desconectado abruptamente.')
        finally:
            with self.connections_lock:
                if conn in self.connections:
                    self.connections.remove(conn)
            conn.close()
            self.log(f'Conexión cerrada con {name}')

    def enviar_mensaje(self, event=None):
        """Envía un mensaje de texto a todos los clientes."""
        mensaje = self.mensaje_text.get(1.0, tk.END).strip()
        self.mensaje_text.delete(1.0, tk.END)
        if mensaje:
            self.broadcast(mensaje.encode())
            self.log(f'Mensaje enviado a los clientes: {mensaje}')
        return "break"

    def enviar_comando(self, tipo, valores):
        """Construye y envía un mensaje de comando a todos los clientes."""
        mensaje = f"{tipo}," + ",".join(str(v) for v in valores)
        self.broadcast(mensaje.encode())
        self.log(f'Comando enviado a clientes: {mensaje}')

    def broadcast(self, message):
        """Envía un mensaje a todas las conexiones activas."""
        with self.connections_lock:
            if not self.connections:
                return
            
            disconnected_clients = []
            for conn in self.connections:
                try:
                    conn.sendall(message)
                except Exception as e:
                    self.log(f"Error al enviar a {conn.getpeername()}: {e}. Eliminando conexión.")
                    disconnected_clients.append(conn)
            
            # Eliminar clientes desconectados de la lista
            for conn in disconnected_clients:
                self.connections.remove(conn)
                conn.close()

    def on_closing(self):
        """Maneja el cierre de la ventana."""
        if self.server_running:
            self.detener_servidor()
        self.master.destroy()

if __name__ == "__main__":
    root = tk.Tk()
    app = TCPServerApp(root)
    root.mainloop()
connections_lock = threading.Lock() # Lock para proteger la lista de conexiones

#FUNCIONES DE CONTROL DE SERVIDOR
def iniciar_Servidor():
    Servidor_Thread=threading.Thread(target=correr_Servidor)
    Servidor_Thread.start()

    Start_Button.config(state=tk.DISABLED)
    Stop_Button.config(state=tk.NORMAL)
    mensaje_Text.config(state=tk.NORMAL)
    BDeliver.config(state=tk.NORMAL)
    BFree.config(state=tk.NORMAL)
    Bimprimir.config(state=tk.NORMAL)
    Balmacenar.config(state=tk.NORMAL)
    Bretirar.config(state=tk.NORMAL)
    BPosicionar.config(state=tk.NORMAL)
    Bretirar_laser.config(state=tk.NORMAL)

    EstadoLabel.config(text='Servidor corriendo',bg="lightgreen")
#Lanza el servidor en un hilo aparte (correr_Servidor), para que la interfaz no se bloquee.

def detener_Servidor():
    global server_running

    server_running = False  # detiene el bucle del servidor

    for conn in connections:
        try:
            conn.close()
        except:
            pass

    try:
        Server_Socket.close()
    except:
        pass

    Start_Button.config(state=tk.NORMAL)
    Stop_Button.config(state=tk.DISABLED)
    BDeliver.config(state=tk.DISABLED)
    BFree.config(state=tk.DISABLED)
    mensaje_Text.config(state=tk.DISABLED)
    Bimprimir.config(state=tk.DISABLED)
    Balmacenar.config(state=tk.DISABLED)
    Bretirar.config(state=tk.DISABLED)
    BPosicionar.config(state=tk.DISABLED)
    Bretirar_laser.config(state=tk.DISABLED)
    EstadoLabel.config(text='Servidor Detenido',bg="red")
    log("Servidor detenido y sockets cerrados.")
    #Marca server_running = False, lo que detiene el bucle principal.
    #Cierra todas las conexiones de clientes y el socket del servidor.
    #Cambia la interfaz a estado "detenido".


def log(mensaje):
    Log_Text.insert(tk.END, mensaje + '\n')
    Log_Text.see(tk.END)
    #Muestra mensajes en el área de texto de la interfaz (log del servidor).

#MANEJO DE CLIENTES

def Manejar_conexion(conn, addr, name):
    log(f'{name} conectado por {addr}')

    while True: 
        #Recibimos los datos del cliente
        data = conn.recv(1024)
        mensaje = data.decode().strip()

        if not mensaje:
            #Cliente termina de enviar el mensaje
            break

        log(f'Datos recibidos de {name}: {mensaje}')

        #Procesamos el mensaje del cliente
        respuesta = f'Recibido: {mensaje}'.encode()

     #Enviamos la respuesta al cliente
        conn.sendall(respuesta)

    # cerrar la conexion
    conn.close()
    with connections_lock:
        if conn in connections:
            connections.remove(conn)
    log(f'Conexión cerrada con {name}')
#Se ejecuta en un hilo separado para cada cliente.
#Recibe mensajes (recv) y los devuelve al cliente como eco (sendall).
#Si el cliente se desconecta, cierra el socket y lo elimina de connections.

#HILO PRINCIPAL DEL SERVIDOR

def correr_Servidor():
    global Server_Socket, connections, server_running
    Server_Socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    Server_Socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        Server_Socket.bind((HOST, PORT))
        Server_Socket.listen(MAX_CONNECTIONS)
        log(f'Servidor corriendo en el puerto {PORT}')
    except OSError as e:
        log(f'Error al iniciar el servidor: {e}')
        return

    connections = []
    names = []
    server_running = True

    while server_running:
        try:
            Server_Socket.settimeout(1.0)  # para salir del accept cada 1 segundo
            conn, addr = Server_Socket.accept()
        except socket.timeout:
            continue
        except OSError:
            break  # el socket ya está cerrado

        with connections_lock:
            connections.append(conn)

        try:
            data = conn.recv(1024)
            name = data.decode().strip()
        except:
            name = "Desconocido"

        names.append(name)

        t = threading.Thread(target=Manejar_conexion, args=(conn, addr, name))
        t.start()
#Crea el socket servidor.
#Hace bind y listen.
#Entra en un bucle donde acepta conexiones (accept).
#Cada cliente se maneja en un nuevo hilo (Manejar_conexion).

#FUNCIONES PARA ENVIAR MENSAJES 

def enviar_Mensaje(event=None):
    mensaje = mensaje_Text.get(1.0, tk.END).strip()
    mensaje_Text.delete(1.0,tk.END)
    if mensaje:
        log(f'Mensaje enviado a los clientes: {mensaje}')
    
    with connections_lock:
        # Copiamos la lista de conexiones de forma segura
        current_connections = list(connections)

    for conn in current_connections:
        try:
            conn.sendall(mensaje.encode())
        except Exception as e:
            log(f"Error al enviar a {conn.getpeername()}: {e}. Eliminando conexión.")
            with connections_lock:
                if conn in connections:
                    connections.remove(conn)
            conn.close() # Cerrar fuera del lock

    if event is not None:
        return "break"

def on_closing():
    try:
        detener_Servidor()
    except:
        pass
    ventana.destroy()
#Envía a todos los clientes lo que el usuario escribe en la caja de texto de la GUI.


def enviar_comando(tipo, valores):
    mensaje = f"{tipo}," + ",".join(str(v) for v in valores)
    log(f'Mensaje a clientes: {mensaje}')

    with connections_lock:
        current_connections = list(connections)

    for conn in current_connections:
        try:
            conn.sendall(mensaje.encode())
        except Exception as e:
            log(f"Error al enviar a {conn.getpeername()}: {e}. Eliminando conexión.")
            with connections_lock:
                if conn in connections:
                    connections.remove(conn)
            conn.close()

#Igual que la anterior, pero usado por los botones de comando (PLC, Laser, etc.), construyendo mensajes específicos.
#(PLC,Laser y almacen corresponden a otras interfaces de tipo cliente desarrolladas por otros practicantes)
# ⚠️ Nota:
# Actualmente existe un comportamiento inesperado: 
# si un cliente se conecta, luego se desconecta y más tarde vuelve a conectarse, 
# puede aparecer el mensaje "Error al enviar mensaje a un cliente. Eliminando conexión",
# incluso aunque el cliente reciba correctamente los mensajes.
# 💡 Desafío: encuentra la causa y propón la solución a este problema, joven padawan.

#INTERFAZ GRAFICA

#Ventana
ventana = tk.Tk()
ventana.title("Servidor")
ventana.geometry("600x600")
ventana.resizable(False, False)

#Frames
Frame_chatservidor = tk.LabelFrame(ventana,text="Chat servidor",font=("Arial", 10, "bold"))
Frame_chatservidor.place(x=5, y=5, width=415, height=300)
Frame_chatservidor.lower()

Frame_PLC=tk.LabelFrame(ventana,text="PLC",font=("Arial", 10, "bold"))
Frame_PLC.place(x=5, y=310, width=190, height=100)

Frame_almacen=tk.LabelFrame(ventana,text="Almacen",font=("Arial", 10, "bold"))
Frame_almacen.place(x=200, y=310, width=220, height=100)

Frame_laser=tk.LabelFrame(ventana,text="Laser",font=("Arial", 10, "bold"))
Frame_laser.place(x=5, y=415, width=210, height=100)



#Label
EstadoLabel = tk.Label(Frame_chatservidor, text='Servidor detenido',bd=1,relief="solid")
EstadoLabel.place(x=50,y=210)

PalletText = tk.Label(Frame_PLC, text="Pallet")
PalletText.place(x= 10,y= 10)

EstacionText = tk.Label(Frame_PLC, text="Estación")
EstacionText.place(x= 10,y= 40)

ArucoLabel=tk.Label(Frame_laser, text="Nº ArUco")
ArucoLabel.place(x=10,y=10)

ArucoLabel_almacen=tk.Label(Frame_almacen, text="Nº ArUco")
ArucoLabel_almacen.place(x=10,y=10)

posicion_almacen=tk.Label(Frame_almacen,text="Posición")
posicion_almacen.place(x= 10,y= 40)

#Text
Log_Text = tk.Text(Frame_chatservidor)
Log_Text.place(x=5, y=10, width=398, height=190)

mensaje_Text = tk.Text(Frame_chatservidor)
mensaje_Text.place(x=5, y=240, width=400, height=20)
mensaje_Text.bind("<Return>", enviar_Mensaje)

#Botones
Start_Button = tk.Button(Frame_chatservidor, text='Iniciar',width=9, command=iniciar_Servidor)
Start_Button.place(x=160, y=210)

Stop_Button = tk.Button(Frame_chatservidor, text='Detener',width=9, command=detener_Servidor, state=tk.DISABLED)
Stop_Button.place(x=240, y=210)

# #Boton
BDeliver = tk.Button(Frame_PLC, text="Deliver",
    command=lambda: enviar_comando("PLC", [estacion.get(), pallet.get(), "deliver"]),
    state=tk.DISABLED)
BDeliver.place(x=130, y=10)

BFree = tk.Button(Frame_PLC, text="Free",
    command=lambda: enviar_comando("PLC", [estacion.get(), pallet.get(), "free"]),
    state=tk.DISABLED)
BFree.place(x=130, y=40)

Bimprimir = tk.Button(Frame_laser, text="Imprimir",
    command=lambda: enviar_comando("Laser,Imprimir",[spinAruco.get()]),
    state=tk.DISABLED)
Bimprimir.place(x=130, y=6)

Balmacenar = tk.Button(Frame_almacen, text="Almacenar",
    command=lambda: enviar_comando("Almacen", [spinPosicion.get(), spinAruco_almacen.get(), "Almacenar"]),
    state=tk.DISABLED)
Balmacenar.place(x=130, y=10)

Bretirar=tk.Button(Frame_almacen, text="Retirar",command=lambda: enviar_comando("Almacen",[spinPosicion.get(),spinAruco_almacen.get(),"Retirar"]),state=tk.DISABLED)
Bretirar.place(x=130,y=40)

BPosicionar=tk.Button(Frame_laser, text="Posicionar",command=lambda: enviar_comando("Laser",["Posicionar"]),state=tk.DISABLED)
BPosicionar.place(x=10,y=40)

Bretirar_laser=tk.Button(Frame_laser, text="Retirar",command=lambda: enviar_comando("Laser",["Retirar"]),state=tk.DISABLED)
Bretirar_laser.place(x=80,y=40)

#Combobox
estacion = ttk.Combobox(
    Frame_PLC,
    state="readonly",
    values = [1,2, 3, 5, 6]
    )
estacion.set(1)
estacion.place(x = 70, y = 40,width = 50, height = 22)

pallet = ttk.Combobox(
    Frame_PLC,
    state="readonly",
    values = [1, 2, 3, 5, 6]
    )
pallet.set(1)
pallet.place(x = 70, y = 10,width=50, height=22)

#Spinbox
spinAruco=tk.Spinbox(Frame_laser,from_=1,to=15,font=("Arial", 12),width=2)
spinAruco.place(x=80,y=10)

spinAruco_almacen=tk.Spinbox(Frame_almacen,from_=1,to=15,font=("Arial", 12),width=2)
spinAruco_almacen.place(x=80,y=10)

spinPosicion=tk.Spinbox(Frame_almacen,from_=1,to=3,font=("Arial", 12),width=2)
spinPosicion.place(x=80,y=40)

ventana.protocol("WM_DELETE_WINDOW", on_closing)
ventana.mainloop()