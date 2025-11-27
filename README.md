Code Manager - Hospital Profamilia 🏥
Departamento: Área de Digitación

Versión: 1.4

Plataforma: Android (Kotlin / Jetpack Compose)

📋 Descripción del Proyecto
Code Manager es una aplicación móvil desarrollada para optimizar, estandarizar y controlar la generación de códigos internos de inventario y servicios dentro del Hospital Profamilia.

La aplicación permite la creación de códigos únicos para insumos y servicios, asegurando la integridad de la base de datos mediante validaciones estrictas contra el inventario físico (Estantes y Refrigeradores) y el catálogo de categorías.

🚀 Funcionalidades Principales
1. Gestión de Códigos
El sistema maneja cuatro tipos principales de códigos con lógicas de generación distintas:

🚑 Emergencia (Prefijo 62): Códigos secuenciales simples. La descripción se prefija automáticamente con //.

🛠️ Servicios (Prefijo 70): Códigos secuenciales simples para servicios generales.

💊 Medicamentos (Prefijo 00): Códigos compuestos. Requiere:

Categoría (ej: Antibióticos).

Ubicación Física (Estante/Refrigerador validado).

Secuencia automática.

consumibles Descartables (Prefijo 01): Códigos compuestos. Misma lógica que medicamentos.

2. Seguridad y Roles de Usuario 🛡️
La aplicación integra Firebase Authentication y gestión de roles mediante Firestore:

👤 Rol Usuario:

Puede generar nuevos códigos.

Visualización de lista de códigos.

Restricción: No puede eliminar, editar, importar ni exportar datos.

🔑 Rol Administrador:

Acceso total al sistema.

Edición de descripciones de códigos.

Eliminación de registros.

Acceso a herramientas de Importación y Exportación Masiva.

3. Validaciones de Integridad 🔒
Para evitar la corrupción de datos ("basura" en la base de datos), el sistema implementa:

Validación de Almacén: Al crear un código compuesto, el usuario debe ingresar el código del estante (ej: 0702 o 3010). El sistema verifica en tiempo real si ese estante existe en la base de datos warehouses.

Validación de Categoría: No se pueden crear ni importar códigos con categorías que no existan en la colección groups.

Anti-Duplicados: El sistema impide la creación o importación de códigos que ya existen.

Descripción Obligatoria: No se permite guardar registros sin descripción.

4. Herramientas de Datos (CSV) 📂
Exportar: Genera un archivo .csv con los códigos filtrados en pantalla.

Importar: Permite carga masiva desde CSV. El sistema detecta automáticamente la secuencia más alta importada para actualizar los contadores y evitar colisiones futuras.

🛠️ Stack Tecnológico
Lenguaje: Kotlin

UI Framework: Jetpack Compose (Material Design 3)

Arquitectura: MVVM (Model-View-ViewModel)

Backend & Base de Datos: Firebase Firestore

Autenticación: Firebase Auth

Concurrencia: Kotlin Coroutines & Flow

📱 Guía de Uso Rápida
Generar un Código Compuesto (Medicamento/Descartable)
Seleccione el filtro (Medicamentos o Descartables).

Presione el botón "Generar Código".

Seleccione la Categoría del desplegable.

Seleccione el tipo de almacenamiento (Estante o Refrigerador).

Escriba el código exacto de la ubicación (Ej: 3010). Si el código no existe en la BD del hospital, el sistema bloqueará la creación.

Ingrese la descripción del producto.

Presione Generar.

Importación Masiva (Solo Administradores)
El archivo CSV debe tener el siguiente formato (separado por comas):

Fragmento de código

code,rootPrefix,categoryCode,warehouseCode,sequence,description,createdBy,createdAt
00-05-3010-0001,00,05,3010,1,Paracetamol 500mg,Admin,171234567890
Si una categoría o código ya existe, la importación omitirá esa línea y reportará el resultado al finalizar.

🔧 Configuración del Entorno (Para Desarrolladores)
Clonar el repositorio.

Asegurarse de tener el archivo google-services.json (proporcionado por el administrador de Firebase de Profamilia) en la carpeta /app.

Sincronizar el proyecto con Gradle.

Ejecutar en un dispositivo Android o Emulador.

📞 Soporte
Para problemas con la base de datos, usuarios bloqueados o nuevos requerimientos:

Área de Digitación - Hospital Profamilia

Contacto Técnico: [vvasquezdv2016@gmail.com]
