#!/usr/bin/env bash

set -Euo pipefail

RUNTIME_ID='jre-25'
INSTALLER_BUILD='unified-installers-v12.2-20260813'
MANIFEST_URL='https://litelauncher.net/api/v1/launcher/java_manifest.json'
LOG_PATH=''
LAST_ERROR=''
PROGRESS_ACTIVE=0
LAST_PROGRESS_LENGTH=0
INSTALLER_FINISHED=0

if [[ -t 1 ]]; then
    COLOR_CYAN=$'\033[36m'
    COLOR_DARK_CYAN=$'\033[2;36m'
    COLOR_DARK_GRAY=$'\033[90m'
    COLOR_GREEN=$'\033[32m'
    COLOR_YELLOW=$'\033[33m'
    COLOR_RED=$'\033[31m'
    COLOR_RESET=$'\033[0m'
else
    COLOR_CYAN=''
    COLOR_DARK_CYAN=''
    COLOR_DARK_GRAY=''
    COLOR_GREEN=''
    COLOR_YELLOW=''
    COLOR_RED=''
    COLOR_RESET=''
fi

detect_installer_language() {
    local language=''
    language="${LC_ALL:-${LC_MESSAGES:-${LANG:-en}}}"
    language="$(printf '%s' "$language" | tr '[:upper:]' '[:lower:]')"
    language="${language%%.*}"
    language="${language%%@*}"
    language="${language%%_*}"
    language="${language%%-*}"
    case "$language" in
        en|es|ru|pt|de|fr|tr|pl|it|uk) printf '%s' "$language" ;;
        *) printf '%s' 'en' ;;
    esac
}

set_language_strings() {
    case "$1" in
        en)
            MSG_ACTION_FAILED='The selected action could not be completed.'
            MSG_ALREADY_INSTALLED='LiteLauncher is already installed.'
            MSG_CHECKING_FOUND='found — existing Java will be kept'
            MSG_CHECKING_JAVA='Checking installed Java...'
            MSG_CHECKING_NOT_FOUND='not found'
            MSG_CREATE_LOCAL_SHORTCUT='Create shortcut in this folder'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Creates or replaces a LiteLauncher shortcut in the folder containing this installer. LiteLauncher will not be reinstalled.'
            MSG_CREATING_SHORTCUT='Creating shortcut...'
            MSG_DONE='Completed.'
            MSG_DOWNLOADING_JAVA='Downloading Java...'
            MSG_ERROR_LABEL='Error: '
            MSG_EXIT_DESC='Closes the installer without making changes.'
            MSG_EXIT_INSTALLER='Exit installer'
            MSG_INSTALLATION_FAILED='LiteLauncher could not be installed.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher has been installed successfully.'
            MSG_INSTALLING_JAVA='Installing Java...'
            MSG_INTERACTIVE_REQUIRED='User input is required because LiteLauncher is already installed.'
            MSG_JAVA_READY='Java is ready.'
            MSG_LOADING_JAVA='Retrieving Java information...'
            MSG_LOG_LABEL='Log:   '
            MSG_NO_CHANGES='No changes were made.'
            MSG_OPEN_LAUNCHER='Open LiteLauncher'
            MSG_OPEN_LAUNCHER_DESC='Starts LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Open shortcut folder'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Opens the folder containing the installed LiteLauncher shortcut.'
            MSG_OPENING_SHORTCUT_FOLDER='Opening shortcut folder...'
            MSG_POST_INSTALL_PROMPT='Select the next action:'
            MSG_POST_OPEN_FOLDER_DESC='Opens the folder containing the installed LiteLauncher shortcut.'
            MSG_PREPARING_FILES='Preparing installation files...'
            MSG_PREPARING_JAVA='Preparing Java...'
            MSG_PRESS_LOG='Press L to open the log, or any other key to close the installer.'
            MSG_PRESS_MAIN='Select an option (1-5): '
            MSG_PRESS_POST='Select an option (1-3): '
            MSG_REINSTALL='Reinstall LiteLauncher'
            MSG_REINSTALL_DESC='Removes the current installation, including Java 25 installed with LiteLauncher, and performs a fresh installation.'
            MSG_REMOVING_LAUNCHER='Removing LiteLauncher...'
            MSG_REMOVING_SHORTCUT='Removing shortcut...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='LiteLauncher shortcut created successfully.'
            MSG_SHORTCUT_LABEL='Shortcut: '
            MSG_STARTING_LAUNCHER='Starting LiteLauncher...'
            MSG_UNINSTALL='Uninstall LiteLauncher'
            MSG_UNINSTALL_DESC='Removes LiteLauncher, Java 25 installed with it, and the installed shortcut.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher has been uninstalled successfully.'
            MSG_VERIFYING_JAVA='verifying'
            MSG_WRITING_BOOTSTRAP='Installing core components...'
            MSG_WRITING_ICONS='Installing icons...'
            MSG_WRITING_LAUNCHER='Installing launcher files...'
            ;;
        es)
            MSG_ACTION_FAILED='No se pudo completar la acción seleccionada.'
            MSG_ALREADY_INSTALLED='LiteLauncher ya está instalado.'
            MSG_CHECKING_FOUND='encontrada — se conservará la instalación existente'
            MSG_CHECKING_JAVA='Comprobando la instalación de Java...'
            MSG_CHECKING_NOT_FOUND='no encontrada'
            MSG_CREATE_LOCAL_SHORTCUT='Crear acceso directo en esta carpeta'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Crea o reemplaza un acceso directo de LiteLauncher en la carpeta que contiene este instalador. LiteLauncher no se reinstalará.'
            MSG_CREATING_SHORTCUT='Creando acceso directo...'
            MSG_DONE='Completado.'
            MSG_DOWNLOADING_JAVA='Descargando Java...'
            MSG_ERROR_LABEL='Error: '
            MSG_EXIT_DESC='Cierra el instalador sin realizar cambios.'
            MSG_EXIT_INSTALLER='Salir del instalador'
            MSG_INSTALLATION_FAILED='No se pudo instalar LiteLauncher.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher se ha instalado correctamente.'
            MSG_INSTALLING_JAVA='Instalando Java...'
            MSG_INTERACTIVE_REQUIRED='Debe seleccionar una acción porque LiteLauncher ya está instalado.'
            MSG_JAVA_READY='Java está listo.'
            MSG_LOADING_JAVA='Obteniendo información de Java...'
            MSG_LOG_LABEL='Registro: '
            MSG_NO_CHANGES='No se realizaron cambios.'
            MSG_OPEN_LAUNCHER='Abrir LiteLauncher'
            MSG_OPEN_LAUNCHER_DESC='Inicia LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Abrir carpeta del acceso directo'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Abre la carpeta que contiene el acceso directo instalado de LiteLauncher.'
            MSG_OPENING_SHORTCUT_FOLDER='Abriendo la carpeta del acceso directo...'
            MSG_POST_INSTALL_PROMPT='Seleccione la siguiente acción:'
            MSG_POST_OPEN_FOLDER_DESC='Abre la carpeta que contiene el acceso directo instalado de LiteLauncher.'
            MSG_PREPARING_FILES='Preparando los archivos de instalación...'
            MSG_PREPARING_JAVA='Preparando Java...'
            MSG_PRESS_LOG='Pulse L para abrir el registro o cualquier otra tecla para cerrar el instalador.'
            MSG_PRESS_MAIN='Seleccione una opción (1-5): '
            MSG_PRESS_POST='Seleccione una opción (1-3): '
            MSG_REINSTALL='Reinstalar LiteLauncher'
            MSG_REINSTALL_DESC='Elimina la instalación actual, incluida la versión de Java 25 instalada con LiteLauncher, y realiza una instalación nueva.'
            MSG_REMOVING_LAUNCHER='Eliminando LiteLauncher...'
            MSG_REMOVING_SHORTCUT='Eliminando acceso directo...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='El acceso directo de LiteLauncher se ha creado correctamente.'
            MSG_SHORTCUT_LABEL='Acceso directo: '
            MSG_STARTING_LAUNCHER='Iniciando LiteLauncher...'
            MSG_UNINSTALL='Desinstalar LiteLauncher'
            MSG_UNINSTALL_DESC='Elimina LiteLauncher, la versión de Java 25 instalada con él y el acceso directo instalado.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher se ha desinstalado correctamente.'
            MSG_VERIFYING_JAVA='verificando'
            MSG_WRITING_BOOTSTRAP='Instalando componentes principales...'
            MSG_WRITING_ICONS='Instalando iconos...'
            MSG_WRITING_LAUNCHER='Instalando archivos de inicio...'
            ;;
        ru)
            MSG_ACTION_FAILED='Не удалось выполнить выбранное действие.'
            MSG_ALREADY_INSTALLED='LiteLauncher уже установлен.'
            MSG_CHECKING_FOUND='найдена — установленная Java будет сохранена'
            MSG_CHECKING_JAVA='Проверка установленной Java...'
            MSG_CHECKING_NOT_FOUND='не найдена'
            MSG_CREATE_LOCAL_SHORTCUT='Создать ярлык в этой папке'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Создаёт или заменяет ярлык LiteLauncher в папке с этим установщиком. Переустановка LiteLauncher не выполняется.'
            MSG_CREATING_SHORTCUT='Создание ярлыка...'
            MSG_DONE='Готово.'
            MSG_DOWNLOADING_JAVA='Загрузка Java...'
            MSG_ERROR_LABEL='Ошибка: '
            MSG_EXIT_DESC='Закрывает установщик без внесения изменений.'
            MSG_EXIT_INSTALLER='Выйти из установщика'
            MSG_INSTALLATION_FAILED='Не удалось установить LiteLauncher.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher успешно установлен.'
            MSG_INSTALLING_JAVA='Установка Java...'
            MSG_INTERACTIVE_REQUIRED='Для продолжения требуется выбрать действие, поскольку LiteLauncher уже установлен.'
            MSG_JAVA_READY='Java готова.'
            MSG_LOADING_JAVA='Получение информации о Java...'
            MSG_LOG_LABEL='Лог:    '
            MSG_NO_CHANGES='Изменения не вносились.'
            MSG_OPEN_LAUNCHER='Открыть LiteLauncher'
            MSG_OPEN_LAUNCHER_DESC='Запускает LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Открыть папку с ярлыком'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Открывает папку, содержащую установленный ярлык LiteLauncher.'
            MSG_OPENING_SHORTCUT_FOLDER='Открытие папки с ярлыком...'
            MSG_POST_INSTALL_PROMPT='Выберите дальнейшее действие:'
            MSG_POST_OPEN_FOLDER_DESC='Открывает папку, содержащую установленный ярлык LiteLauncher.'
            MSG_PREPARING_FILES='Подготовка файлов установки...'
            MSG_PREPARING_JAVA='Подготовка Java...'
            MSG_PRESS_LOG='Нажмите L, чтобы открыть лог, или любую другую клавишу, чтобы закрыть установщик.'
            MSG_PRESS_MAIN='Выберите вариант (1-5): '
            MSG_PRESS_POST='Выберите вариант (1-3): '
            MSG_REINSTALL='Переустановить LiteLauncher'
            MSG_REINSTALL_DESC='Удаляет текущую установку, включая Java 25, установленную вместе с LiteLauncher, и выполняет чистую переустановку.'
            MSG_REMOVING_LAUNCHER='Удаление LiteLauncher...'
            MSG_REMOVING_SHORTCUT='Удаление ярлыка...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='Ярлык LiteLauncher успешно создан.'
            MSG_SHORTCUT_LABEL='Ярлык: '
            MSG_STARTING_LAUNCHER='Запуск LiteLauncher...'
            MSG_UNINSTALL='Удалить LiteLauncher'
            MSG_UNINSTALL_DESC='Удаляет LiteLauncher, Java 25, установленную вместе с ним, и установленный ярлык.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher успешно удалён.'
            MSG_VERIFYING_JAVA='проверка'
            MSG_WRITING_BOOTSTRAP='Установка основных компонентов...'
            MSG_WRITING_ICONS='Установка значков...'
            MSG_WRITING_LAUNCHER='Установка файлов запуска...'
            ;;
        pt)
            MSG_ACTION_FAILED='Não foi possível concluir a ação selecionada.'
            MSG_ALREADY_INSTALLED='LiteLauncher já está instalado.'
            MSG_CHECKING_FOUND='encontrada — a instalação existente será mantida'
            MSG_CHECKING_JAVA='Verificando a instalação do Java...'
            MSG_CHECKING_NOT_FOUND='não encontrada'
            MSG_CREATE_LOCAL_SHORTCUT='Criar atalho nesta pasta'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Cria ou substitui um atalho do LiteLauncher na pasta deste instalador. O LiteLauncher não será reinstalado.'
            MSG_CREATING_SHORTCUT='Criando atalho...'
            MSG_DONE='Concluído.'
            MSG_DOWNLOADING_JAVA='Baixando Java...'
            MSG_ERROR_LABEL='Erro: '
            MSG_EXIT_DESC='Fecha o instalador sem fazer alterações.'
            MSG_EXIT_INSTALLER='Sair do instalador'
            MSG_INSTALLATION_FAILED='Não foi possível instalar o LiteLauncher.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher foi instalado com sucesso.'
            MSG_INSTALLING_JAVA='Instalando Java...'
            MSG_INTERACTIVE_REQUIRED='É necessário selecionar uma ação porque o LiteLauncher já está instalado.'
            MSG_JAVA_READY='Java está pronto.'
            MSG_LOADING_JAVA='Obtendo informações do Java...'
            MSG_LOG_LABEL='Log:   '
            MSG_NO_CHANGES='Nenhuma alteração foi feita.'
            MSG_OPEN_LAUNCHER='Abrir LiteLauncher'
            MSG_OPEN_LAUNCHER_DESC='Inicia o LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Abrir pasta do atalho'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Abre a pasta que contém o atalho instalado do LiteLauncher.'
            MSG_OPENING_SHORTCUT_FOLDER='Abrindo a pasta do atalho...'
            MSG_POST_INSTALL_PROMPT='Selecione a próxima ação:'
            MSG_POST_OPEN_FOLDER_DESC='Abre a pasta que contém o atalho instalado do LiteLauncher.'
            MSG_PREPARING_FILES='Preparando os arquivos de instalação...'
            MSG_PREPARING_JAVA='Preparando Java...'
            MSG_PRESS_LOG='Pressione L para abrir o log ou qualquer outra tecla para fechar o instalador.'
            MSG_PRESS_MAIN='Selecione uma opção (1-5): '
            MSG_PRESS_POST='Selecione uma opção (1-3): '
            MSG_REINSTALL='Reinstalar LiteLauncher'
            MSG_REINSTALL_DESC='Remove a instalação atual, incluindo o Java 25 instalado com o LiteLauncher, e realiza uma nova instalação.'
            MSG_REMOVING_LAUNCHER='Removendo LiteLauncher...'
            MSG_REMOVING_SHORTCUT='Removendo atalho...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='O atalho do LiteLauncher foi criado com sucesso.'
            MSG_SHORTCUT_LABEL='Atalho: '
            MSG_STARTING_LAUNCHER='Iniciando LiteLauncher...'
            MSG_UNINSTALL='Desinstalar LiteLauncher'
            MSG_UNINSTALL_DESC='Remove o LiteLauncher, o Java 25 instalado com ele e o atalho instalado.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher foi desinstalado com sucesso.'
            MSG_VERIFYING_JAVA='verificando'
            MSG_WRITING_BOOTSTRAP='Instalando componentes principais...'
            MSG_WRITING_ICONS='Instalando ícones...'
            MSG_WRITING_LAUNCHER='Instalando arquivos de inicialização...'
            ;;
        de)
            MSG_ACTION_FAILED='Die ausgewählte Aktion konnte nicht abgeschlossen werden.'
            MSG_ALREADY_INSTALLED='LiteLauncher ist bereits installiert.'
            MSG_CHECKING_FOUND='gefunden — vorhandenes Java wird beibehalten'
            MSG_CHECKING_JAVA='Installiertes Java wird geprüft...'
            MSG_CHECKING_NOT_FOUND='nicht gefunden'
            MSG_CREATE_LOCAL_SHORTCUT='Verknüpfung in diesem Ordner erstellen'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Erstellt oder ersetzt eine LiteLauncher-Verknüpfung im Ordner dieses Installers. LiteLauncher wird nicht neu installiert.'
            MSG_CREATING_SHORTCUT='Verknüpfung wird erstellt...'
            MSG_DONE='Abgeschlossen.'
            MSG_DOWNLOADING_JAVA='Java wird heruntergeladen...'
            MSG_ERROR_LABEL='Fehler: '
            MSG_EXIT_DESC='Schließt den Installer, ohne Änderungen vorzunehmen.'
            MSG_EXIT_INSTALLER='Installer beenden'
            MSG_INSTALLATION_FAILED='LiteLauncher konnte nicht installiert werden.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher wurde erfolgreich installiert.'
            MSG_INSTALLING_JAVA='Java wird installiert...'
            MSG_INTERACTIVE_REQUIRED='Da LiteLauncher bereits installiert ist, muss eine Aktion ausgewählt werden.'
            MSG_JAVA_READY='Java ist bereit.'
            MSG_LOADING_JAVA='Java-Informationen werden abgerufen...'
            MSG_LOG_LABEL='Log:    '
            MSG_NO_CHANGES='Es wurden keine Änderungen vorgenommen.'
            MSG_OPEN_LAUNCHER='LiteLauncher öffnen'
            MSG_OPEN_LAUNCHER_DESC='Startet LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Ordner mit Verknüpfung öffnen'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Öffnet den Ordner mit der installierten LiteLauncher-Verknüpfung.'
            MSG_OPENING_SHORTCUT_FOLDER='Ordner mit Verknüpfung wird geöffnet...'
            MSG_POST_INSTALL_PROMPT='Wählen Sie die nächste Aktion:'
            MSG_POST_OPEN_FOLDER_DESC='Öffnet den Ordner mit der installierten LiteLauncher-Verknüpfung.'
            MSG_PREPARING_FILES='Installationsdateien werden vorbereitet...'
            MSG_PREPARING_JAVA='Java wird vorbereitet...'
            MSG_PRESS_LOG='Drücken Sie L, um das Protokoll zu öffnen, oder eine andere Taste, um den Installer zu schließen.'
            MSG_PRESS_MAIN='Wählen Sie eine Option (1-5): '
            MSG_PRESS_POST='Wählen Sie eine Option (1-3): '
            MSG_REINSTALL='LiteLauncher neu installieren'
            MSG_REINSTALL_DESC='Entfernt die aktuelle Installation einschließlich des mit LiteLauncher installierten Java 25 und führt eine Neuinstallation durch.'
            MSG_REMOVING_LAUNCHER='LiteLauncher wird entfernt...'
            MSG_REMOVING_SHORTCUT='Verknüpfung wird entfernt...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='Die LiteLauncher-Verknüpfung wurde erfolgreich erstellt.'
            MSG_SHORTCUT_LABEL='Verknüpfung: '
            MSG_STARTING_LAUNCHER='LiteLauncher wird gestartet...'
            MSG_UNINSTALL='LiteLauncher deinstallieren'
            MSG_UNINSTALL_DESC='Entfernt LiteLauncher, das mitinstallierte Java 25 und die installierte Verknüpfung.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher wurde erfolgreich deinstalliert.'
            MSG_VERIFYING_JAVA='wird überprüft'
            MSG_WRITING_BOOTSTRAP='Kernkomponenten werden installiert...'
            MSG_WRITING_ICONS='Symbole werden installiert...'
            MSG_WRITING_LAUNCHER='Launcher-Dateien werden installiert...'
            ;;
        fr)
            MSG_ACTION_FAILED='L'"'"'action sélectionnée n'"'"'a pas pu être effectuée.'
            MSG_ALREADY_INSTALLED='LiteLauncher est déjà installé.'
            MSG_CHECKING_FOUND='trouvée — l'"'"'installation existante sera conservée'
            MSG_CHECKING_JAVA='Vérification de l'"'"'installation Java...'
            MSG_CHECKING_NOT_FOUND='introuvable'
            MSG_CREATE_LOCAL_SHORTCUT='Créer un raccourci dans ce dossier'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Crée ou remplace un raccourci LiteLauncher dans le dossier contenant cet installateur. LiteLauncher ne sera pas réinstallé.'
            MSG_CREATING_SHORTCUT='Création du raccourci...'
            MSG_DONE='Terminé.'
            MSG_DOWNLOADING_JAVA='Téléchargement de Java...'
            MSG_ERROR_LABEL='Erreur : '
            MSG_EXIT_DESC='Ferme l'"'"'installateur sans apporter de modification.'
            MSG_EXIT_INSTALLER='Quitter l'"'"'installateur'
            MSG_INSTALLATION_FAILED='LiteLauncher n'"'"'a pas pu être installé.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher a été installé avec succès.'
            MSG_INSTALLING_JAVA='Installation de Java...'
            MSG_INTERACTIVE_REQUIRED='Vous devez sélectionner une action, car LiteLauncher est déjà installé.'
            MSG_JAVA_READY='Java est prêt.'
            MSG_LOADING_JAVA='Récupération des informations Java...'
            MSG_LOG_LABEL='Journal : '
            MSG_NO_CHANGES='Aucune modification n'"'"'a été effectuée.'
            MSG_OPEN_LAUNCHER='Ouvrir LiteLauncher'
            MSG_OPEN_LAUNCHER_DESC='Démarre LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Ouvrir le dossier du raccourci'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Ouvre le dossier contenant le raccourci LiteLauncher installé.'
            MSG_OPENING_SHORTCUT_FOLDER='Ouverture du dossier du raccourci...'
            MSG_POST_INSTALL_PROMPT='Sélectionnez l'"'"'action suivante :'
            MSG_POST_OPEN_FOLDER_DESC='Ouvre le dossier contenant le raccourci LiteLauncher installé.'
            MSG_PREPARING_FILES='Préparation des fichiers d'"'"'installation...'
            MSG_PREPARING_JAVA='Préparation de Java...'
            MSG_PRESS_LOG='Appuyez sur L pour ouvrir le journal, ou sur une autre touche pour fermer l'"'"'installateur.'
            MSG_PRESS_MAIN='Sélectionnez une option (1-5) : '
            MSG_PRESS_POST='Sélectionnez une option (1-3) : '
            MSG_REINSTALL='Réinstaller LiteLauncher'
            MSG_REINSTALL_DESC='Supprime l'"'"'installation actuelle, y compris Java 25 installé avec LiteLauncher, puis effectue une nouvelle installation.'
            MSG_REMOVING_LAUNCHER='Suppression de LiteLauncher...'
            MSG_REMOVING_SHORTCUT='Suppression du raccourci...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='Le raccourci LiteLauncher a été créé avec succès.'
            MSG_SHORTCUT_LABEL='Raccourci : '
            MSG_STARTING_LAUNCHER='Démarrage de LiteLauncher...'
            MSG_UNINSTALL='Désinstaller LiteLauncher'
            MSG_UNINSTALL_DESC='Supprime LiteLauncher, la version de Java 25 installée avec LiteLauncher et le raccourci installé.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher a été désinstallé avec succès.'
            MSG_VERIFYING_JAVA='vérification'
            MSG_WRITING_BOOTSTRAP='Installation des composants principaux...'
            MSG_WRITING_ICONS='Installation des icônes...'
            MSG_WRITING_LAUNCHER='Installation des fichiers de lancement...'
            ;;
        tr)
            MSG_ACTION_FAILED='Seçilen işlem tamamlanamadı.'
            MSG_ALREADY_INSTALLED='LiteLauncher zaten yüklü.'
            MSG_CHECKING_FOUND='bulundu — mevcut Java kurulumu korunacak'
            MSG_CHECKING_JAVA='Yüklü Java kontrol ediliyor...'
            MSG_CHECKING_NOT_FOUND='bulunamadı'
            MSG_CREATE_LOCAL_SHORTCUT='Bu klasörde kısayol oluştur'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Bu yükleyicinin bulunduğu klasörde LiteLauncher kısayolu oluşturur veya mevcut kısayolu değiştirir. LiteLauncher yeniden yüklenmez.'
            MSG_CREATING_SHORTCUT='Kısayol oluşturuluyor...'
            MSG_DONE='Tamamlandı.'
            MSG_DOWNLOADING_JAVA='Java indiriliyor...'
            MSG_ERROR_LABEL='Hata: '
            MSG_EXIT_DESC='Değişiklik yapmadan yükleyiciyi kapatır.'
            MSG_EXIT_INSTALLER='Yükleyiciden çık'
            MSG_INSTALLATION_FAILED='LiteLauncher yüklenemedi.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher başarıyla yüklendi.'
            MSG_INSTALLING_JAVA='Java kuruluyor...'
            MSG_INTERACTIVE_REQUIRED='LiteLauncher zaten yüklü olduğundan bir işlem seçmeniz gerekiyor.'
            MSG_JAVA_READY='Java hazır.'
            MSG_LOADING_JAVA='Java bilgileri alınıyor...'
            MSG_LOG_LABEL='Günlük: '
            MSG_NO_CHANGES='Hiçbir değişiklik yapılmadı.'
            MSG_OPEN_LAUNCHER='LiteLauncher'"'"'ı aç'
            MSG_OPEN_LAUNCHER_DESC='LiteLauncher'"'"'ı başlatır.'
            MSG_OPEN_SHORTCUT_FOLDER='Kısayol klasörünü aç'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Yüklü LiteLauncher kısayolunu içeren klasörü açar.'
            MSG_OPENING_SHORTCUT_FOLDER='Kısayol klasörü açılıyor...'
            MSG_POST_INSTALL_PROMPT='Sonraki işlemi seçin:'
            MSG_POST_OPEN_FOLDER_DESC='Yüklü LiteLauncher kısayolunu içeren klasörü açar.'
            MSG_PREPARING_FILES='Kurulum dosyaları hazırlanıyor...'
            MSG_PREPARING_JAVA='Java hazırlanıyor...'
            MSG_PRESS_LOG='Günlüğü açmak için L tuşuna, yükleyiciyi kapatmak için başka bir tuşa basın.'
            MSG_PRESS_MAIN='Bir seçenek belirleyin (1-5): '
            MSG_PRESS_POST='Bir seçenek belirleyin (1-3): '
            MSG_REINSTALL='LiteLauncher'"'"'ı yeniden yükle'
            MSG_REINSTALL_DESC='LiteLauncher ile yüklenen Java 25 dahil mevcut kurulumu kaldırır ve temiz bir kurulum yapar.'
            MSG_REMOVING_LAUNCHER='LiteLauncher kaldırılıyor...'
            MSG_REMOVING_SHORTCUT='Kısayol kaldırılıyor...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='LiteLauncher kısayolu başarıyla oluşturuldu.'
            MSG_SHORTCUT_LABEL='Kısayol: '
            MSG_STARTING_LAUNCHER='LiteLauncher başlatılıyor...'
            MSG_UNINSTALL='LiteLauncher'"'"'ı kaldır'
            MSG_UNINSTALL_DESC='LiteLauncher'"'"'ı, onunla birlikte yüklenen Java 25'"'"'i ve yüklü kısayolu kaldırır.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher başarıyla kaldırıldı.'
            MSG_VERIFYING_JAVA='doğrulanıyor'
            MSG_WRITING_BOOTSTRAP='Temel bileşenler yükleniyor...'
            MSG_WRITING_ICONS='Simgeler yükleniyor...'
            MSG_WRITING_LAUNCHER='Başlatma dosyaları yükleniyor...'
            ;;
        pl)
            MSG_ACTION_FAILED='Nie udało się wykonać wybranej operacji.'
            MSG_ALREADY_INSTALLED='LiteLauncher jest już zainstalowany.'
            MSG_CHECKING_FOUND='znaleziona — istniejąca instalacja Javy zostanie zachowana'
            MSG_CHECKING_JAVA='Sprawdzanie zainstalowanej Javy...'
            MSG_CHECKING_NOT_FOUND='nie znaleziono'
            MSG_CREATE_LOCAL_SHORTCUT='Utwórz skrót w tym folderze'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Tworzy lub zastępuje skrót LiteLauncher w folderze tego instalatora. LiteLauncher nie zostanie ponownie zainstalowany.'
            MSG_CREATING_SHORTCUT='Tworzenie skrótu...'
            MSG_DONE='Gotowe.'
            MSG_DOWNLOADING_JAVA='Pobieranie Javy...'
            MSG_ERROR_LABEL='Błąd: '
            MSG_EXIT_DESC='Zamyka instalator bez wprowadzania zmian.'
            MSG_EXIT_INSTALLER='Zamknij instalator'
            MSG_INSTALLATION_FAILED='Nie udało się zainstalować LiteLauncher.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher został pomyślnie zainstalowany.'
            MSG_INSTALLING_JAVA='Instalowanie Javy...'
            MSG_INTERACTIVE_REQUIRED='Ponieważ LiteLauncher jest już zainstalowany, należy wybrać działanie.'
            MSG_JAVA_READY='Java jest gotowa.'
            MSG_LOADING_JAVA='Odczytywanie informacji o Javie...'
            MSG_LOG_LABEL='Log:   '
            MSG_NO_CHANGES='Nie wprowadzono żadnych zmian.'
            MSG_OPEN_LAUNCHER='Otwórz LiteLauncher'
            MSG_OPEN_LAUNCHER_DESC='Uruchamia LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Otwórz folder skrótu'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Otwiera folder zawierający zainstalowany skrót LiteLauncher.'
            MSG_OPENING_SHORTCUT_FOLDER='Otwieranie folderu skrótu...'
            MSG_POST_INSTALL_PROMPT='Wybierz dalsze działanie:'
            MSG_POST_OPEN_FOLDER_DESC='Otwiera folder zawierający zainstalowany skrót LiteLauncher.'
            MSG_PREPARING_FILES='Przygotowywanie plików instalacyjnych...'
            MSG_PREPARING_JAVA='Przygotowywanie Javy...'
            MSG_PRESS_LOG='Naciśnij L, aby otworzyć log, lub dowolny inny klawisz, aby zamknąć instalator.'
            MSG_PRESS_MAIN='Wybierz opcję (1-5): '
            MSG_PRESS_POST='Wybierz opcję (1-3): '
            MSG_REINSTALL='Zainstaluj LiteLauncher ponownie'
            MSG_REINSTALL_DESC='Usuwa bieżącą instalację, w tym Javę 25 zainstalowaną wraz z LiteLauncher, a następnie wykonuje nową instalację.'
            MSG_REMOVING_LAUNCHER='Usuwanie LiteLauncher...'
            MSG_REMOVING_SHORTCUT='Usuwanie skrótu...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='Skrót LiteLauncher został pomyślnie utworzony.'
            MSG_SHORTCUT_LABEL='Skrót: '
            MSG_STARTING_LAUNCHER='Uruchamianie LiteLauncher...'
            MSG_UNINSTALL='Odinstaluj LiteLauncher'
            MSG_UNINSTALL_DESC='Usuwa LiteLauncher, Javę 25 zainstalowaną wraz z nim oraz zainstalowany skrót.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher został pomyślnie odinstalowany.'
            MSG_VERIFYING_JAVA='weryfikacja'
            MSG_WRITING_BOOTSTRAP='Instalowanie głównych składników...'
            MSG_WRITING_ICONS='Instalowanie ikon...'
            MSG_WRITING_LAUNCHER='Instalowanie plików uruchomieniowych...'
            ;;
        it)
            MSG_ACTION_FAILED='Impossibile completare l'"'"'azione selezionata.'
            MSG_ALREADY_INSTALLED='LiteLauncher è già installato.'
            MSG_CHECKING_FOUND='trovata — l'"'"'installazione esistente verrà mantenuta'
            MSG_CHECKING_JAVA='Controllo dell'"'"'installazione Java...'
            MSG_CHECKING_NOT_FOUND='non trovata'
            MSG_CREATE_LOCAL_SHORTCUT='Crea collegamento in questa cartella'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Crea o sostituisce un collegamento LiteLauncher nella cartella di questo installer. LiteLauncher non verrà reinstallato.'
            MSG_CREATING_SHORTCUT='Creazione del collegamento...'
            MSG_DONE='Completato.'
            MSG_DOWNLOADING_JAVA='Download di Java...'
            MSG_ERROR_LABEL='Errore: '
            MSG_EXIT_DESC='Chiude l'"'"'installer senza apportare modifiche.'
            MSG_EXIT_INSTALLER='Esci dall'"'"'installer'
            MSG_INSTALLATION_FAILED='Impossibile installare LiteLauncher.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher è stato installato correttamente.'
            MSG_INSTALLING_JAVA='Installazione di Java...'
            MSG_INTERACTIVE_REQUIRED='Poiché LiteLauncher è già installato, è necessario selezionare un'"'"'azione.'
            MSG_JAVA_READY='Java è pronto.'
            MSG_LOADING_JAVA='Recupero delle informazioni su Java...'
            MSG_LOG_LABEL='Log:    '
            MSG_NO_CHANGES='Non è stata apportata alcuna modifica.'
            MSG_OPEN_LAUNCHER='Apri LiteLauncher'
            MSG_OPEN_LAUNCHER_DESC='Avvia LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Apri cartella del collegamento'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Apre la cartella contenente il collegamento LiteLauncher installato.'
            MSG_OPENING_SHORTCUT_FOLDER='Apertura della cartella del collegamento...'
            MSG_POST_INSTALL_PROMPT='Seleziona l'"'"'azione successiva:'
            MSG_POST_OPEN_FOLDER_DESC='Apre la cartella contenente il collegamento LiteLauncher installato.'
            MSG_PREPARING_FILES='Preparazione dei file di installazione...'
            MSG_PREPARING_JAVA='Preparazione di Java...'
            MSG_PRESS_LOG='Premi L per aprire il log o qualsiasi altro tasto per chiudere l'"'"'installer.'
            MSG_PRESS_MAIN='Seleziona un'"'"'opzione (1-5): '
            MSG_PRESS_POST='Seleziona un'"'"'opzione (1-3): '
            MSG_REINSTALL='Reinstalla LiteLauncher'
            MSG_REINSTALL_DESC='Rimuove l'"'"'installazione corrente, incluso Java 25 installato con LiteLauncher, quindi esegue una nuova installazione.'
            MSG_REMOVING_LAUNCHER='Rimozione di LiteLauncher...'
            MSG_REMOVING_SHORTCUT='Rimozione del collegamento...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='Il collegamento LiteLauncher è stato creato correttamente.'
            MSG_SHORTCUT_LABEL='Collegamento: '
            MSG_STARTING_LAUNCHER='Avvio di LiteLauncher...'
            MSG_UNINSTALL='Disinstalla LiteLauncher'
            MSG_UNINSTALL_DESC='Rimuove LiteLauncher, la versione di Java 25 installata con LiteLauncher e il collegamento installato.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher è stato disinstallato correttamente.'
            MSG_VERIFYING_JAVA='verifica'
            MSG_WRITING_BOOTSTRAP='Installazione dei componenti principali...'
            MSG_WRITING_ICONS='Installazione delle icone...'
            MSG_WRITING_LAUNCHER='Installazione dei file di avvio...'
            ;;
        uk)
            MSG_ACTION_FAILED='Не вдалося виконати вибрану дію.'
            MSG_ALREADY_INSTALLED='LiteLauncher уже встановлено.'
            MSG_CHECKING_FOUND='знайдено — наявну Java буде збережено'
            MSG_CHECKING_JAVA='Перевірка встановленої Java...'
            MSG_CHECKING_NOT_FOUND='не знайдено'
            MSG_CREATE_LOCAL_SHORTCUT='Створити ярлик у цій папці'
            MSG_CREATE_LOCAL_SHORTCUT_DESC='Створює або замінює ярлик LiteLauncher у папці з цим інсталятором. Перевстановлення LiteLauncher не виконується.'
            MSG_CREATING_SHORTCUT='Створення ярлика...'
            MSG_DONE='Готово.'
            MSG_DOWNLOADING_JAVA='Завантаження Java...'
            MSG_ERROR_LABEL='Помилка: '
            MSG_EXIT_DESC='Закриває інсталятор без внесення змін.'
            MSG_EXIT_INSTALLER='Вийти з інсталятора'
            MSG_INSTALLATION_FAILED='Не вдалося встановити LiteLauncher.'
            MSG_INSTALLED_SUCCESSFULLY='LiteLauncher успішно встановлено.'
            MSG_INSTALLING_JAVA='Встановлення Java...'
            MSG_INTERACTIVE_REQUIRED='Для продовження потрібно вибрати дію, оскільки LiteLauncher уже встановлено.'
            MSG_JAVA_READY='Java готова.'
            MSG_LOADING_JAVA='Отримання інформації про Java...'
            MSG_LOG_LABEL='Лог:     '
            MSG_NO_CHANGES='Зміни не вносилися.'
            MSG_OPEN_LAUNCHER='Відкрити LiteLauncher'
            MSG_OPEN_LAUNCHER_DESC='Запускає LiteLauncher.'
            MSG_OPEN_SHORTCUT_FOLDER='Відкрити папку з ярликом'
            MSG_OPEN_SHORTCUT_FOLDER_DESC='Відкриває папку, що містить встановлений ярлик LiteLauncher.'
            MSG_OPENING_SHORTCUT_FOLDER='Відкриття папки з ярликом...'
            MSG_POST_INSTALL_PROMPT='Виберіть подальшу дію:'
            MSG_POST_OPEN_FOLDER_DESC='Відкриває папку, що містить встановлений ярлик LiteLauncher.'
            MSG_PREPARING_FILES='Підготовка файлів інсталяції...'
            MSG_PREPARING_JAVA='Підготовка Java...'
            MSG_PRESS_LOG='Натисніть L, щоб відкрити лог, або будь-яку іншу клавішу, щоб закрити інсталятор.'
            MSG_PRESS_MAIN='Виберіть варіант (1-5): '
            MSG_PRESS_POST='Виберіть варіант (1-3): '
            MSG_REINSTALL='Перевстановити LiteLauncher'
            MSG_REINSTALL_DESC='Видаляє поточне встановлення, включно з Java 25, встановленою разом із LiteLauncher, і виконує чисте перевстановлення.'
            MSG_REMOVING_LAUNCHER='Видалення LiteLauncher...'
            MSG_REMOVING_SHORTCUT='Видалення ярлика...'
            MSG_SHORTCUT_CREATED_SUCCESSFULLY='Ярлик LiteLauncher успішно створено.'
            MSG_SHORTCUT_LABEL='Ярлик: '
            MSG_STARTING_LAUNCHER='Запуск LiteLauncher...'
            MSG_UNINSTALL='Видалити LiteLauncher'
            MSG_UNINSTALL_DESC='Видаляє LiteLauncher, Java 25, встановлену разом із ним, і встановлений ярлик.'
            MSG_UNINSTALLED_SUCCESSFULLY='LiteLauncher успішно видалено.'
            MSG_VERIFYING_JAVA='перевірка'
            MSG_WRITING_BOOTSTRAP='Встановлення основних компонентів...'
            MSG_WRITING_ICONS='Встановлення значків...'
            MSG_WRITING_LAUNCHER='Встановлення файлів запуску...'
            ;;
    esac
}

INSTALLER_LANGUAGE="$(detect_installer_language)"
set_language_strings "$INSTALLER_LANGUAGE"
utc_timestamp() {
    date -u '+%Y-%m-%dT%H:%M:%SZ'
}

write_log() {
    local level="$1"
    local message="$2"
    [[ -n "$LOG_PATH" ]] || return 0
    {
        printf '[%s] %s  %s\n' "$(utc_timestamp)" "$level" "$message"
    } >> "$LOG_PATH" 2>/dev/null || true
}

initialize_log() {
    local path="$1"
    mkdir -p "$(dirname "$path")"
    printf 'LiteLauncher Installer log - %s\n' "$(utc_timestamp)" > "$path"
}

write_banner() {
    printf '\n'
    printf '%s  ==============================================================%s\n' "$COLOR_DARK_GRAY" "$COLOR_RESET"
    printf '%s                         LiteLauncher Installer%s\n' "$COLOR_CYAN" "$COLOR_RESET"
    printf '%s                                  Linux%s\n' "$COLOR_DARK_CYAN" "$COLOR_RESET"
    printf '%s  ==============================================================%s\n' "$COLOR_DARK_GRAY" "$COLOR_RESET"
    printf '\n'
}

write_progress() {
    local percent="$1"
    local status="$2"
    local color="${3:-$COLOR_CYAN}"
    local value filled empty bar line padding

    value="$percent"
    (( value < 0 )) && value=0
    (( value > 100 )) && value=100
    filled=$((38 * value / 100))
    empty=$((38 - filled))
    bar="$(printf '%*s' "$filled" '' | tr ' ' '#')$(printf '%*s' "$empty" '' | tr ' ' '-')"
    line=$(printf '  [%s] %3d%%  %s' "$bar" "$value" "$status")

    if (( ${#line} < LAST_PROGRESS_LENGTH )); then
        padding=$((LAST_PROGRESS_LENGTH - ${#line}))
        line+="$(printf '%*s' "$padding" '')"
    fi
    LAST_PROGRESS_LENGTH=${#line}

    printf '\r%s%s%s' "$color" "$line" "$COLOR_RESET"
    PROGRESS_ACTIVE=1
}

complete_progress_line() {
    if (( PROGRESS_ACTIVE == 1 )); then
        printf '\n'
        PROGRESS_ACTIVE=0
        LAST_PROGRESS_LENGTH=0
    fi
}

write_stage() {
    local percent="$1"
    local status="$2"
    local color="${3:-$COLOR_CYAN}"
    write_progress "$percent" "$status" "$color"
    write_log 'INFO' "Progress ${percent}%: ${status}"
}

die() {
    LAST_ERROR="$1"
    return 1
}

remove_path_quietly() {
    local path="${1:-}"
    [[ -n "$path" ]] || return 0
    rm -rf "$path" >/dev/null 2>&1 || true
}

file_size() {
    wc -c < "$1" | tr -d '[:space:]'
}

sha1_file() {
    local path="$1"
    case "${SHA1_TOOL:-}" in
        sha1sum) sha1sum "$path" | awk '{print tolower($1)}' ;;
        shasum) shasum -a 1 "$path" | awk '{print tolower($1)}' ;;
        openssl) openssl sha1 "$path" | awk '{print tolower($NF)}' ;;
        *) die 'SHA-1 verification is unavailable on this Linux installation.' ;;
    esac
}

decode_base64_to() {
    local target="$1"
    local temporary="${target}.litelauncher-install"
    mkdir -p "$(dirname "$target")"
    remove_path_quietly "$temporary"

    if base64 --decode </dev/null >/dev/null 2>&1; then
        if ! base64 --decode > "$temporary"; then
            remove_path_quietly "$temporary"
            die "Unable to decode embedded installer resource: ${target}."
        fi
    elif base64 -d </dev/null >/dev/null 2>&1; then
        if ! base64 -d > "$temporary"; then
            remove_path_quietly "$temporary"
            die "Unable to decode embedded installer resource: ${target}."
        fi
    elif base64 -D </dev/null >/dev/null 2>&1; then
        if ! base64 -D > "$temporary"; then
            remove_path_quietly "$temporary"
            die "Unable to decode embedded installer resource: ${target}."
        fi
    else
        die 'The installed base64 utility does not support decoding.'
    fi

    if ! mv -f "$temporary" "$target"; then
        remove_path_quietly "$temporary"
        die "Unable to write embedded installer resource: ${target}."
    fi
}

write_bootstrap_payload() {
    decode_base64_to "$1" <<'__LL_BOOTSTRAP_BASE64__'
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
cnyGMQfwPDED/r2i53LktJP6IOoZ/WZG3449fNt24hAvqFHEKL+fGvbhOXTjVRzQeUkhcJ3bNSXk
8HeKpTN40RFCp4mg09fViatAVwM31l4nCP0L6Bp/Iqbxx3+o1aVbREZHEX+bLuN1ZvaHcXaHN4ab
LuLH03izqXQqp4G5475Co6+HsfWyPyS4+rmO+wrUv/nbCLo/kPMdPf5Kj1f1+Bs9/laPv8ffNFwF
/o5/4t+o/z9QSwcIBgxK2R4IAAAlEAAAUEsDBAoAAAgAAHaYc10AAAAAAAAAAAAAAAAiAAAAbmV0
L2xpdGVsYXVuY2hlci9iYWNrZW5kL2Rvd25sb2FkL1BLAwQUAAgICAB2mHNdAAAAAAAAAAAAAAAA
OQAAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9kb3dubG9hZC9Eb3dubG9hZEV4Y2VwdGlvbi5j
bGFzc42QTU/CQBCG36WlhVK+hRs3D7QYG+MR9eLHiRgTCAdupd1Ateyapog/youJhsSDP8AfZZyW
Rkjk4GVm9t153tmdr++PTwDnaBvIQdGhmshDYzgVPHbCIOahuxTenEfO1PUeuPAdX65EKF3fucqK
62ePP8aBFAzaWSCC+IKh1x3cu0+uE7pi5gzjKBCz/o4ymkdy5U5D3p9YYwMFFHUYyeQSQ3PbtuNs
/9fQGhfpJ2UTFVQZ6p4UgnuJx10k6X7BwCYMrT121phBvZQ+Z6gOAsFvl4spj0aJK4PStQgzhnIZ
efwmSKT2nwUcJ544ofkFWipDI3kEVSrVedQo1ul0SOcc5YLdY8oa7DXrBYyNDh3FlCjtEMqGONpH
mESUU0LLiCPqV5MbItbQ7c47zC1VSd1q1F2nGQ1SK2hmZIcyo5y332C+/CJaKrYoHlDOofUDUEsH
CAPCDv9PAQAAOgIAAFBLAwQUAAgICAB2mHNdAAAAAAAAAAAAAAAANAAAAG5ldC9saXRlbGF1bmNo
ZXIvYmFja2VuZC9kb3dubG9hZC9Eb3dubG9hZEZpbGUuY2xhc3OVVF1z20QUPZvIXttRcOLEJmpI
XKANttxELeUzbkvTpKUJThqaTmHgBdleYjWyZCS5DDzxb3iBGTqDh5nMwCsz/CjgriS7zlhkyMPd
j6uzZ+/VPXv/+vv0DwC3cZTDFKY5FBUppBnmnpnPTcM2nWPjsWi5XpshfctyrOAOw3Sl+jSDDAOy
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
FE88Js8c3pEmuIwSsKy5Nc8wpyxGou2MRx/LMdZPo+qkG1scu8QuXYBkPJBKIFuZJK80lyq7F4Bk
J7siSW8h/6coEM7npF0jyT7yLpL7aHuN9h3SBHxBzxh/FyX/biwjFPiHuFVWhcwBLr2GUO//JXvS
v9+DnOlUIcxDb5OMkFQyvV2v8G7mzi+4/tNR8Dl6C049SRTPc5AEmZJ480efr8AAvyY+OlYC/l0B
/8gubp1l0kbPCRiB592AeSKzi/CPECMvEAm/5JDHIRdR5iWEWsYEMpgNo8W3RXedjh5vGC2/Lfrq
6egbWA2iuyG86WzmZ1w/W2QPZHZwA9dtQgyRHMn8CUnNvIBMGW/v4/18rzq0j49+gEjV7/Sqo4eK
SMrYoSLv7CKxc8Srg9gAKZpGGleQQS99h2iOZT6paHxKxMDfSNBTUZIyuwIDGoN80MRc/B3CUphS
aks0IVFbilISbSlGabT6xZGwhvUA4jueG5j9jRDUh2E19wqTewhz9ZOIOspVkavTUXWMqzJXP42p
Ga4mDvB4D0m1qNKufnaAxT3cOt64Tn48YlSJSNVJ9LsXp5+WKB7S/oWCZbOwweWzfwFQSwcIlIAQ
lWEEAAApCQAAUEsDBAoAAAgAAHaYc10AAAAAAAAAAAAAAAAbAAAAbmV0L2xpdGVsYXVuY2hlci9i
b290c3RyYXAvUEsDBBQACAgIAHaYc10AAAAAAAAAAAAAAAA/AAAAbmV0L2xpdGVsYXVuY2hlci9i
b290c3RyYXAvQm9vdHN0cmFwJEJvb3RzdHJhcFNjZW5lJFN0YWdlLmNsYXNzpVNhT9NQFD1v3ehW
C4yJIBNFYOo6lKGIxmwQEKeSDEYokBA/deM5Skpr2jc++4M0UUiEaDR89kcZ72sKLMIHw5Zs597b
c+85u33v95/vvwDM4rmKGMMLl4uiYwvuWC23scP9Yt3zRCB860Px5WmUO4vMBnd5zhRWk6fAENeR
QBdDcmP11cL60sobhlL1yhNLcmRSR0qOTFTW1mprsnJNh45uBjW3uVDdqJgM5XcdiGjoRVpFt44+
ZEin4XguZ+jPG9Vda98qOpbbLNbqu7whiNuPGyoGdAziJkPPOaHitvbI0r7ltHjtPcNMvq170bGC
oNRWMIVvu81Su4IcEM7P6riFYYausu3aYo5hIH+xc8nYTCKh0TaySaQkjuoYw7jcSughYJjLG53s
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
2QmKec4CjUrDECRJscAJ1YpF3Wzs/Q2cokgNB8qGVhOBMxRLPBD0Axydo1jmaFp669taqXqfRy5Q
pIW5pJllvcazz/Dss+xePHuZ/kWKNW6LygO2q029ppd4aJ1CHQ7t1PR63Q9tUOREhkP2qxR5waSP
YKLYfcQKFzOcjm32n7bsXsNquYyEC21XditS7/Z7bfuuwzmVxV7nRcUGO3SMtZ8iyfvDZkleV6Hz
UhekpqQuSV2Wmpa6JlWVmpOaFzoGwr8r9t5kqxRTwjSwOgB5xyYE19g76EOEcf2PlaywGf8df0Do
QRw/xPg5wOQhqI+OxfgqUIIjJcGeObGe8y3fxPgu0EkfvY3jk9IWZFFsehHHayUvwGkBmsqBMivW
Z/0tR4qrWAKcH7mMUhhg5RCro/C5gJf+By+PwDg+C3rlXyFyrCO8HDPsk0qzcmwiggqm8JBxl/Xs
JWJ4xTr2htV2gASOmJPghqjzzd9QSwcIgeqpIE0CAADTAwAAUEsDBBQACAgIAHaYc10AAAAAAAAA
AAAAAAAuAAAAbmV0L2xpdGVsYXVuY2hlci91aS9QaXhlbEJ1dHRvbiRSZW5kZXJlci5jbGFzc42Q
wUoDMRCG/6ntblur1nrqyYtCvbgPUASrUFwoWrrQe7qOmhKzkE3EZ/PgA/hQ4uwq1EvROeQfhj/5
v8zH59s7gAscxWgQRpZ9YrRno4LNn9glQSdz/crmKnhf2JMF23t27GI0Cf21elGJUfYxuVutOfeE
yNUGwnw02/pWFtyDynmcSm13/SRmXnkeny0J3awILuepNizZvzznFQdhuAjW62de6lKvDE+sLeSu
LmxJOJ5tWKcSVI2VSa3nbxRC+5ZLf1OUPsae+P/AIvRSa9ldG1WWLAHtzWYOCaf/+hWhVWtEIOyg
KpK1thBJ10QsZwNtdEQjcXRFd+tZD/uiA+mEAwdRB33RweXwC1BLBwj5htGPCwEAAM8BAABQSwME
FAAICAgAdphzXQAAAAAAAAAAAAAAACsAAABuZXQvbGl0ZWxhdW5jaGVyL3VpL1BpeGVsQnV0dG9u
JFN0YXRlLmNsYXNzjVNtT9NQFH7utq5bLW7ypiA4RJBuKBMEBTcJbzOQDEYozix+KuMKJaVLupb4
0Z+kIxGi0fDZH2U891LDIiRuy3Z2Tp+3nt79+v3tJ4DXKKqIMIy73M87ts8dK3Drh9zLB3Z+2/7I
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
3SU8wyVa46SBT7hM65hBrkHJqFCgJEAJgIKDAgMD9KEoEFwFPU1aXTTMPdrjywF/A/QBuKIBnwc+
a8B5aksbfIbKUe1JmBf3MKVDM8qy43osch0zv8LQQZNaaL8OmtZCUzpoRgsd1EGHtdAhHXREC83q
oONa6OhBaAzXSHfhOm6E+iZuK67AiLjXYR4PB87DkfNw6Dwcu1rvhFaGJ1B/kbuwcJ9+jjN4iEd4
jGJ4X3HwJp4hi+dYwgqyvwBQSwcImdJcJzEDAABFBwAAUEsDBBQACAgIAHaYc10AAAAAAAAAAAAA
AAAnAAAAbmV0L2xpdGVsYXVuY2hlci91aS90ZXh0L1RleHRGb250LmNsYXNzlZ0FoBbVuv7XxP6m
NjILLFJApRSxA7AoQSmFjQIKbmATsglhg2C32N2d2IUidncdu7u7u/7v8zzfPR7v9dxz/zfWw37n
N/PNt2at9c5a83zjI7/fcodzbnN3R+5CFyUurnU1rhK4crf6BfU9G+tnTe05fOJuDZOaAlfpM33W
9KYtAhd17TY6d6nLEpfXusLVBq7jrIamno3Tmxoa6+fPmjStYW7P+dN7NjUsbOo5yoqBs2fZ/lnj
7PrJg2fWT20I3Ppdh/z5ASOb5k6fNbV3N4Xq92jqOR1Uz77zp0xpmNugfXpn9okr1LrmrgxcTX1T
Y/28wLX/z7u0qHUt3YqBC/oFrvu4If/xPNfYpnHRnGncdeVat4pbNXDJ0MEjRw4etk3guv1/7d+6
1rVxbe2j+wcuHDcyce0D1+X/eIDctXMdcC06Bi7vOlj/0200Drt6rVvDrWmHHYrD9kOoS63ryi+5
TeK6/+XiqW5zt7brkbh1al1Pt27gWnL7rOmze06Z3tjQc0R90zSrzGhqg12l3n9zZcb924v1l2P0
Ttz6gVv1rxuGTJ81Y/icpumzZ+VuQ7dR4jaudZu4Tf/HSQy0wk6i0rBw+rymeX+2kL9+wrgh/+7o
vbuNRZvsVet6uz7W2OY2/LOxbfi3h/oPzS111tKDnrnr5raqdVu7vnYd5jXVz22at+P0pmmBW+nv
WvBY4P1r3QA30E5h3vyJ87gB9OBu/5PP3SA3OHHb1rrt3JDANf8T6GfN2yphBbsk/OcQ6zoNcwPX
quu/HuZfNtmxhrnhiRtR67Z3O9gH/i1ltW5H3KFh3uz5cyc1bD3PzqOhfqa163/fHa3OBs+aM79J
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
AAAAAAAAAAAAzBQAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9Cb290c3RyYXBMb2cuY2xhc3NQ
SwECCgAKAAAIAAB2mHNdAAAAAAAAAAAAAAAAIgAAAAAAAAAAAAAAAABDHQAAbmV0L2xpdGVsYXVu
Y2hlci9iYWNrZW5kL2Rvd25sb2FkL1BLAQIUABQACAgIAHaYc10Dwg7/TwEAADoCAAA5AAAAAAAA
AAAAAAAAAIMdAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRFeGNl
cHRpb24uY2xhc3NQSwECFAAUAAgICAB2mHNdTKkB6qUDAAAyCAAANAAAAAAAAAAAAAAAAAA5HwAA
bmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2Rvd25sb2FkL0Rvd25sb2FkRmlsZS5jbGFzc1BLAQIU
ABQACAgIAHaYc11BdD/2zAAAABkBAAA4AAAAAAAAAAAAAAAAAEAjAABuZXQvbGl0ZWxhdW5jaGVy
L2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRQcm9ncmVzcy5jbGFzc1BLAQIUABQACAgIAHaYc125
cQ+xSwMAABEIAABJAAAAAAAAAAAAAAAAAHIkAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93
bmxvYWQvRG93bmxvYWRTZXJ2aWNlJFZhbGlkYXRpb25TdW1tYXJ5LmNsYXNzUEsBAhQAFAAICAgA
dphzXfyAcUmlIAAAFEgAADcAAAAAAAAAAAAAAAAANCgAAG5ldC9saXRlbGF1bmNoZXIvYmFja2Vu
ZC9kb3dubG9hZC9Eb3dubG9hZFNlcnZpY2UuY2xhc3NQSwECCgAKAAAIAAB2mHNdAAAAAAAAAAAA
AAAAHgAAAAAAAAAAAAAAAAA+SQAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2phdmEvUEsBAhQA
FAAICAgAdphzXZHW1TIuBwAA6g4AADYAAAAAAAAAAAAAAAAAekkAAG5ldC9saXRlbGF1bmNoZXIv
YmFja2VuZC9qYXZhL0phdmFSdW50aW1lTG9jYXRvci5jbGFzc1BLAQIUABQACAgIAHaYc10ThWOA
twoAADgUAABJAAAAAAAAAAAAAAAAAAxRAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvamF2YS9K
YXZhUnVudGltZU1hbmlmZXN0U2VydmljZSRKc29uUGFyc2VyLmNsYXNzUEsBAhQAFAAICAgAdphz
Xanu0cNuDgAAUxwAAD4AAAAAAAAAAAAAAAAAOlwAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9q
YXZhL0phdmFSdW50aW1lTWFuaWZlc3RTZXJ2aWNlLmNsYXNzUEsBAhQAFAAICAgAdphzXdHD8xgz
AwAAFgYAADYAAAAAAAAAAAAAAAAAFGsAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9qYXZhL0ph
dmFSdW50aW1lUGFja2FnZS5jbGFzc1BLAQIUABQACAgIAHaYc10VYJIuWhYAAJwwAAA2AAAAAAAA
AAAAAAAAAKtuAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvamF2YS9KYXZhUnVudGltZVNlcnZp
Y2UuY2xhc3NQSwECCgAKAAAIAAB2mHNdAAAAAAAAAAAAAAAAIgAAAAAAAAAAAAAAAABphQAAbmV0
L2xpdGVsYXVuY2hlci9iYWNrZW5kL3BsYXRmb3JtL1BLAQIUABQACAgIAHaYc12iU/aBywMAAJ0H
AAA1AAAAAAAAAAAAAAAAAKmFAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvcGxhdGZvcm0vTGF1
bmNoZXJQYXRocy5jbGFzc1BLAQIUABQACAgIAHaYc10AE1gwdwgAAHsQAAAvAAAAAAAAAAAAAAAA
ANeJAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvcGxhdGZvcm0vT1NVdGlscy5jbGFzc1BLAQIU
ABQACAgIAHaYc12UgBCVYQQAACkJAAA3AAAAAAAAAAAAAAAAAKuSAABuZXQvbGl0ZWxhdW5jaGVy
L2JhY2tlbmQvcGxhdGZvcm0vT3BlcmF0aW5nU3lzdGVtLmNsYXNzUEsBAgoACgAACAAAdphzXQAA
AAAAAAAAAAAAABsAAAAAAAAAAAAAAAAAcZcAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL1BL
AQIUABQACAgIAHaYc11v88N6aAIAAEgFAAA/AAAAAAAAAAAAAAAAAKqXAABuZXQvbGl0ZWxhdW5j
aGVyL2Jvb3RzdHJhcC9Cb290c3RyYXAkQm9vdHN0cmFwU2NlbmUkU3RhZ2UuY2xhc3NQSwECFAAU
AAgICAB2mHNdSb2hp/IHAADyEAAAOQAAAAAAAAAAAAAAAAB/mgAAbmV0L2xpdGVsYXVuY2hlci9i
b290c3RyYXAvQm9vdHN0cmFwJEJvb3RzdHJhcFNjZW5lLmNsYXNzUEsBAhQAFAAICAgAdphzXd7z
57ssBAAAbwkAADYAAAAAAAAAAAAAAAAA2KIAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL0Jv
b3RzdHJhcCRCb290c3RyYXBVaS5jbGFzc1BLAQIUABQACAgIAHaYc13v4ZYT9QQAABQLAAAqAAAA
AAAAAAAAAAAAAGinAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290c3RyYXAuY2xhc3NQ
SwECFAAUAAgICAB2mHNdv+PTqqsMAADoGwAAMQAAAAAAAAAAAAAAAAC1rAAAbmV0L2xpdGVsYXVu
Y2hlci9ib290c3RyYXAvQm9vdHN0cmFwQmFja2VuZC5jbGFzc1BLAQIUABQACAgIAHaYc13e3Kaf
8AAAAG8BAAAzAAAAAAAAAAAAAAAAAL+5AABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290
c3RyYXBFeGNlcHRpb24uY2xhc3NQSwECFAAUAAgICAB2mHNdZAKC17ALAAB3GwAANwAAAAAAAAAA
AAAAAAAQuwAAbmV0L2xpdGVsYXVuY2hlci9ib290c3RyYXAvTGF1bmNoZXJJbnN0YWxsU2Vydmlj
ZS5jbGFzc1BLAQIUABQACAgIAHaYc104yrbnvQoAALIWAAAxAAAAAAAAAAAAAAAAACXHAABuZXQv
bGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9MYXVuY2hlck1hbmlmZXN0LmNsYXNzUEsBAhQAFAAICAgA
dphzXXAgdMMSDQAAZRwAADgAAAAAAAAAAAAAAAAAQdIAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0
cmFwL0xhdW5jaGVyTWFuaWZlc3RTZXJ2aWNlLmNsYXNzUEsBAhQAFAAICAgAdphzXW5b2Fh8AgAA
sAUAAC0AAAAAAAAAAAAAAAAAud8AAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL01hbmlmZXN0
TG9hZC5jbGFzc1BLAQIKAAoAAAgAAHaYc10AAAAAAAAAAAAAAAAUAAAAAAAAAAAAAAAAAJDiAABu
ZXQvbGl0ZWxhdW5jaGVyL3VpL1BLAQIUABQACAgIAHaYc10mua9Q5QUAAJwKAAAjAAAAAAAAAAAA
AAAAAMLiAABuZXQvbGl0ZWxhdW5jaGVyL3VpL0FwcFdpbmRvdy5jbGFzc1BLAQIUABQACAgIAHaY
c13/V4+8+AMAABwHAAAhAAAAAAAAAAAAAAAAAPjoAABuZXQvbGl0ZWxhdW5jaGVyL3VpL0hvdHNw
b3QuY2xhc3NQSwECFAAUAAgICAB2mHNdsY2gRgUDAACzBQAAJQAAAAAAAAAAAAAAAAA/7QAAbmV0
L2xpdGVsYXVuY2hlci91aS9Nb3VzZUN1cnNvci5jbGFzc1BLAQIUABQACAgIAHaYc13bIHuzxQMA
AKYHAAAkAAAAAAAAAAAAAAAAAJfwAABuZXQvbGl0ZWxhdW5jaGVyL3VpL01vdXNlU3RhdGUuY2xh
c3NQSwECFAAUAAgICAB2mHNdgeqpIE0CAADTAwAAIQAAAAAAAAAAAAAAAACu9AAAbmV0L2xpdGVs
YXVuY2hlci91aS9QYWxldHRlLmNsYXNzUEsBAhQAFAAICAgAdphzXfmG0Y8LAQAAzwEAAC4AAAAA
AAAAAAAAAAAASvcAAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxCdXR0b24kUmVuZGVyZXIuY2xh
c3NQSwECFAAUAAgICAB2mHNd2DuqOIICAADgBAAAKwAAAAAAAAAAAAAAAACx+AAAbmV0L2xpdGVs
YXVuY2hlci91aS9QaXhlbEJ1dHRvbiRTdGF0ZS5jbGFzc1BLAQIUABQACAgIAHaYc101Xm9VgwUA
AJgKAAAlAAAAAAAAAAAAAAAAAIz7AABuZXQvbGl0ZWxhdW5jaGVyL3VpL1BpeGVsQnV0dG9uLmNs
YXNzUEsBAhQAFAAICAgAdphzXSvaUvR2AwAAtAYAADIAAAAAAAAAAAAAAAAAYgEBAG5ldC9saXRl
bGF1bmNoZXIvdWkvUGl4ZWxDYW52YXMkSW5wdXRIYW5kbGVyLmNsYXNzUEsBAhQAFAAICAgAdphz
XSr7nku2BwAAow4AACUAAAAAAAAAAAAAAAAAOAUBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxD
YW52YXMuY2xhc3NQSwECFAAUAAgICAB2mHNdrkooDcINAABgGgAAJwAAAAAAAAAAAAAAAABBDQEA
bmV0L2xpdGVsYXVuY2hlci91aS9QaXhlbEdyYXBoaWNzLmNsYXNzUEsBAhQAFAAICAgAdphzXbHz
TFPCBwAArhEAACYAAAAAAAAAAAAAAAAAWBsBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxQYWlu
dGVyLmNsYXNzUEsBAhQAFAAICAgAdphzXVmaPNP2AQAAuwMAACYAAAAAAAAAAAAAAAAAbiMBAG5l
dC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxTdXJmYWNlLmNsYXNzUEsBAhQAFAAICAgAdphzXTru2xJn
AgAAtAQAAC0AAAAAAAAAAAAAAAAAuCUBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxUZXh0JEFs
aWdubWVudC5jbGFzc1BLAQIUABQACAgIAHaYc12OrgBgTAcAAMcNAAAjAAAAAAAAAAAAAAAAAHoo
AQBuZXQvbGl0ZWxhdW5jaGVyL3VpL1BpeGVsVGV4dC5jbGFzc1BLAQIUABQACAgIAHaYc13vdnVu
IgIAAFQDAAAlAAAAAAAAAAAAAAAAABcwAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL1Byb2dyZXNzQmFy
LmNsYXNzUEsBAhQAFAAICAgAdphzXYxucALBAAAA8QAAACYAAAAAAAAAAAAAAAAAjDIBAG5ldC9s
aXRlbGF1bmNoZXIvdWkvVGFza1Byb2dyZXNzLmNsYXNzUEsBAhQAFAAICAgAdphzXTwuS4+MBgAA
FA4AACYAAAAAAAAAAAAAAAAAoTMBAG5ldC9saXRlbGF1bmNoZXIvdWkvV2luZG93Q2hyb21lLmNs
YXNzUEsBAgoACgAACAAAdphzXQAAAAAAAAAAAAAAABkAAAAAAAAAAAAAAAAAgToBAG5ldC9saXRl
bGF1bmNoZXIvdWkvdGV4dC9QSwECFAAUAAgICAB2mHNdQQw2T9ACAACqBAAAKgAAAAAAAAAAAAAA
AAC4OgEAbmV0L2xpdGVsYXVuY2hlci91aS90ZXh0L0dseXBoTGF5b3V0LmNsYXNzUEsBAhQAFAAI
CAgAdphzXZnSXCcxAwAARQcAAC0AAAAAAAAAAAAAAAAA4D0BAG5ldC9saXRlbGF1bmNoZXIvdWkv
dGV4dC9UZXh0Rm9udCRHbHlwaC5jbGFzc1BLAQIUABQACAgIAHaYc12wehGgjDwAAPt7AAAnAAAA
AAAAAAAAAAAAAGxBAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL3RleHQvVGV4dEZvbnQuY2xhc3NQSwEC
FAAUAAgICAB2mHNdyNZo/qsGAAAHDAAALQAAAAAAAAAAAAAAAABNfgEAbmV0L2xpdGVsYXVuY2hl
ci91aS90ZXh0L1RleHRSYXN0ZXJpemVyLmNsYXNzUEsFBgAAAAA+AD4A7RQAAFOFAQAAAA==
__LL_BOOTSTRAP_BASE64__
}

write_icon_payload() {
    decode_base64_to "$1" <<'__LL_ICON_BASE64__'
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
__LL_ICON_BASE64__
}

get_architecture() {
    local machine
    machine="$(uname -m)"
    case "$machine" in
        x86_64|amd64) ARCHITECTURE='x64' ;;
        aarch64|arm64) ARCHITECTURE='aarch64' ;;
        *) die "Unsupported Linux architecture: ${machine}." ;;
    esac
}

get_paths() {
    HOME_DIRECTORY="${HOME:-}"
    [[ -n "$HOME_DIRECTORY" ]] || die 'The user home directory could not be determined.'

    MINECRAFT_DIRECTORY="${HOME_DIRECTORY}/.minecraft"
    LOGS_DIRECTORY="${MINECRAFT_DIRECTORY}/logs"
    LOG_FILE="${LOGS_DIRECTORY}/litelauncher_installer.log"
    LITELAUNCHER_DIRECTORY="${MINECRAFT_DIRECTORY}/litelauncher"
    JAVA_DIRECTORY="${LITELAUNCHER_DIRECTORY}/java"
    JAVA_ROOT="${JAVA_DIRECTORY}/${RUNTIME_ID}"
    JAVA_TEMP_ROOT="${JAVA_DIRECTORY}/${RUNTIME_ID}.tmp"
    BOOTSTRAP_DIRECTORY="${LITELAUNCHER_DIRECTORY}/bootstrap"
    BOOTSTRAP_JAR="${BOOTSTRAP_DIRECTORY}/Bootstrap.jar"
    LAUNCHER_DIRECTORY="${BOOTSTRAP_DIRECTORY}/launcher"
    ICONS_DIRECTORY="${BOOTSTRAP_DIRECTORY}/icons"
    ICON_FILE="${ICONS_DIRECTORY}/launcher.png"
    UNIX_SCRIPT="${BOOTSTRAP_DIRECTORY}/launch-litelauncher.sh"
    INSTALLER_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
    LOCAL_DESKTOP_FILE="${INSTALLER_DIRECTORY}/LiteLauncher.desktop"

    XDG_DATA_DIRECTORY="${XDG_DATA_HOME:-${HOME_DIRECTORY}/.local/share}"
    APPLICATIONS_DIRECTORY="${XDG_DATA_DIRECTORY}/applications"
    HICOLOR_DIRECTORY="${XDG_DATA_DIRECTORY}/icons/hicolor"
    APPLICATION_ICON="${HICOLOR_DIRECTORY}/128x128/apps/net.litelauncher.LiteLauncher.png"
    DESKTOP_FILE="${APPLICATIONS_DIRECTORY}/net.litelauncher.LiteLauncher.desktop"
}

litelauncher_installation_exists() {
    [[ -e "$LITELAUNCHER_DIRECTORY" || -L "$LITELAUNCHER_DIRECTORY" ]]
}

read_installer_action() {
    local key=''
    if ! litelauncher_installation_exists; then
        printf '%s' 'install'
        return 0
    fi
    printf '%s  %s%s\n' "$COLOR_YELLOW" "$MSG_ALREADY_INSTALLED" "$COLOR_RESET" >&2
    printf '\n' >&2
    printf '%s  [1] %s%s\n' "$COLOR_CYAN" "$MSG_REINSTALL" "$COLOR_RESET" >&2
    printf '%s      %s%s\n' "$COLOR_DARK_GRAY" "$MSG_REINSTALL_DESC" "$COLOR_RESET" >&2
    printf '%s  [2] %s%s\n' "$COLOR_CYAN" "$MSG_UNINSTALL" "$COLOR_RESET" >&2
    printf '%s      %s%s\n' "$COLOR_DARK_GRAY" "$MSG_UNINSTALL_DESC" "$COLOR_RESET" >&2
    printf '%s  [3] %s%s\n' "$COLOR_CYAN" "$MSG_CREATE_LOCAL_SHORTCUT" "$COLOR_RESET" >&2
    printf '%s      %s%s\n' "$COLOR_DARK_GRAY" "$MSG_CREATE_LOCAL_SHORTCUT_DESC" "$COLOR_RESET" >&2
    printf '%s  [4] %s%s\n' "$COLOR_CYAN" "$MSG_OPEN_SHORTCUT_FOLDER" "$COLOR_RESET" >&2
    printf '%s      %s%s\n' "$COLOR_DARK_GRAY" "$MSG_OPEN_SHORTCUT_FOLDER_DESC" "$COLOR_RESET" >&2
    printf '%s  [5] %s%s\n' "$COLOR_CYAN" "$MSG_EXIT_INSTALLER" "$COLOR_RESET" >&2
    printf '%s      %s%s\n' "$COLOR_DARK_GRAY" "$MSG_EXIT_DESC" "$COLOR_RESET" >&2
    printf '\n' >&2
    if [[ ! -t 0 ]]; then
        die "$MSG_INTERACTIVE_REQUIRED"
        return 1
    fi
    while true; do
        printf '%s  %s%s' "$COLOR_YELLOW" "$MSG_PRESS_MAIN" "$COLOR_RESET" >&2
        IFS= read -r -n 1 key || true
        printf '%s\n' "$key" >&2
        case "$key" in
            1) printf '%s' 'reinstall'; return 0 ;;
            2) printf '%s' 'uninstall'; return 0 ;;
            3) printf '%s' 'create-local-shortcut'; return 0 ;;
            4) printf '%s' 'open-shortcut-folder'; return 0 ;;
            5) printf '%s' 'exit'; return 0 ;;
        esac
    done
}

remove_path_completely() {
    local path="$1"
    local description="$2"
    local attempt

    [[ -e "$path" || -L "$path" ]] || return 0
    attempt=1
    while (( attempt <= 3 )); do
        rm -rf "$path" >/dev/null 2>&1 || true
        if [[ ! -e "$path" && ! -L "$path" ]]; then
            return 0
        fi
        sleep "0.${attempt}"
        attempt=$((attempt + 1))
    done

    die "Unable to remove ${description}: ${path}"
}

remove_desktop_app_if_present() {
    local removed=0
    if [[ -e "$DESKTOP_FILE" || -L "$DESKTOP_FILE" ]]; then
        remove_path_completely "$DESKTOP_FILE" 'the LiteLauncher application shortcut'
        write_log 'INFO' "Application shortcut removed: ${DESKTOP_FILE}"
        removed=1
    else
        write_log 'INFO' "Application shortcut was not present: ${DESKTOP_FILE}"
    fi

    if [[ -e "$APPLICATION_ICON" || -L "$APPLICATION_ICON" ]]; then
        remove_path_completely "$APPLICATION_ICON" 'the LiteLauncher application icon'
        write_log 'INFO' "Application icon removed: ${APPLICATION_ICON}"
        removed=1
    else
        write_log 'INFO' "Application icon was not present: ${APPLICATION_ICON}"
    fi

    if (( removed == 1 )); then
        refresh_linux_desktop_caches
    fi
}

initialize_operation_log() {
    local action="$1"
    LOG_PATH="$LOG_FILE"
    initialize_log "$LOG_PATH"
    write_log 'INFO' "LiteLauncher Linux installer started. Action: ${action}"
    write_log 'INFO' "Installer build: ${INSTALLER_BUILD}"
    write_log 'INFO' "Installer language: ${INSTALLER_LANGUAGE}"
    write_log 'INFO' "Kernel: $(uname -sr 2>/dev/null || printf 'unknown')"
    write_log 'INFO' "Architecture: $(uname -m 2>/dev/null || printf 'unknown')"
    write_log 'INFO' "Shell: ${BASH_VERSION}"
    write_log 'INFO' "Desktop: ${XDG_CURRENT_DESKTOP:-unknown}"
    write_log 'INFO' "Minecraft directory: ${MINECRAFT_DIRECTORY}"
    write_log 'INFO' "XDG data directory: ${XDG_DATA_DIRECTORY}"
}

prepare_full_reinstallation() {
    write_stage 2 "$MSG_REMOVING_SHORTCUT" "$COLOR_YELLOW"
    remove_desktop_app_if_present
    write_stage 4 "$MSG_REMOVING_LAUNCHER" "$COLOR_YELLOW"
    remove_path_completely "$LITELAUNCHER_DIRECTORY" 'the existing LiteLauncher installation'
    write_log 'INFO' "Existing installation removed completely: ${LITELAUNCHER_DIRECTORY}"
}

uninstall_litelauncher() {
    write_stage 20 "$MSG_REMOVING_LAUNCHER" "$COLOR_YELLOW"
    remove_path_completely "$LITELAUNCHER_DIRECTORY" 'the LiteLauncher installation'
    write_stage 75 "$MSG_REMOVING_SHORTCUT" "$COLOR_YELLOW"
    remove_desktop_app_if_present
    write_stage 100 "$MSG_DONE" "$COLOR_GREEN"
    complete_progress_line
    printf '\n'
    printf '%s  %s%s\n' "$COLOR_GREEN" "$MSG_UNINSTALLED_SUCCESSFULLY" "$COLOR_RESET"
    write_log 'INFO' 'Uninstallation completed successfully.'
    INSTALLER_FINISHED=1
    sleep 0.65
}

ensure_required_tools() {
    local tool
    for tool in uname date mkdir dirname rm mv cat awk sed tr head tail wc tar base64 chmod cp touch ls sleep kill nohup env; do
        command -v "$tool" >/dev/null 2>&1 || die "Required Linux tool is unavailable: ${tool}."
    done

    if command -v curl >/dev/null 2>&1; then
        DOWNLOADER='curl'
    elif command -v wget >/dev/null 2>&1; then
        DOWNLOADER='wget'
    else
        die 'Either curl or wget is required to download Java.'
    fi

    if command -v python3 >/dev/null 2>&1; then
        JSON_PARSER='python3'
    elif command -v python >/dev/null 2>&1 && python -c 'import sys; raise SystemExit(0 if sys.version_info[0] == 3 else 1)' >/dev/null 2>&1; then
        JSON_PARSER='python'
    elif command -v perl >/dev/null 2>&1 && perl -MJSON::PP -e 1 >/dev/null 2>&1; then
        JSON_PARSER='perl'
    elif command -v jq >/dev/null 2>&1; then
        JSON_PARSER='jq'
    else
        die 'A JSON parser is required: Python 3, Perl with JSON::PP, or jq.'
    fi

    if command -v sha1sum >/dev/null 2>&1; then
        SHA1_TOOL='sha1sum'
    elif command -v shasum >/dev/null 2>&1; then
        SHA1_TOOL='shasum'
    elif command -v openssl >/dev/null 2>&1; then
        SHA1_TOOL='openssl'
    else
        die 'SHA-1 verification is unavailable. Install coreutils, shasum, or OpenSSL.'
    fi

    write_log 'INFO' "Downloader: ${DOWNLOADER}; JSON parser: ${JSON_PARSER}; SHA-1 tool: ${SHA1_TOOL}"
}

ensure_directories() {
    mkdir -p \
        "$BOOTSTRAP_DIRECTORY" \
        "$LAUNCHER_DIRECTORY" \
        "$JAVA_DIRECTORY" \
        "$ICONS_DIRECTORY" \
        "$APPLICATIONS_DIRECTORY" \
        "$(dirname "$APPLICATION_ICON")" \
        "$LOGS_DIRECTORY"
    write_log 'INFO' "Install directory: ${BOOTSTRAP_DIRECTORY}"
}

find_java() {
    local root="$1"
    local direct_java="${root}/bin/java"
    local bundle_java="${root}/Contents/Home/bin/java"

    # This function is used only for a runtime freshly extracted by this
    # installer. Existing jre-25 contents are deliberately never probed.
    if [[ -f "$direct_java" ]]; then
        printf '%s' "$direct_java"
        return 0
    fi
    if [[ -f "$bundle_java" ]]; then
        printf '%s' "$bundle_java"
        return 0
    fi
    return 1
}

java_runtime_entry_exists() {
    # Installation is entered only after a clean start or after the complete
    # reinstall branch removed the whole litelauncher directory. Java must not
    # be preserved in this pipeline.
    if [[ -e "$JAVA_ROOT" || -L "$JAVA_ROOT" ]]; then
        write_log 'ERROR' "Unexpected Java runtime entry remained before installation: ${JAVA_ROOT}"
        return 0
    fi
    write_log 'INFO' "Java runtime directory entry is absent: ${JAVA_ROOT}"
    return 1
}

cleanup_java_temporary_files() {
    local archive="${1:-}"
    remove_path_quietly "$JAVA_TEMP_ROOT"
    if [[ -n "$archive" ]]; then
        remove_path_quietly "$archive"
        remove_path_quietly "${archive}.litelauncher-download"
        remove_path_quietly "${archive}.curl-error"
        remove_path_quietly "${archive}.download-error"
        remove_path_quietly "${archive}.download-status"
    fi
    remove_path_quietly "${JAVA_DIRECTORY}/${RUNTIME_ID}.zip"
    remove_path_quietly "${JAVA_DIRECTORY}/${RUNTIME_ID}.tar.gz"
    remove_path_quietly "${JAVA_DIRECTORY}/${RUNTIME_ID}.zip.litelauncher-download"
    remove_path_quietly "${JAVA_DIRECTORY}/${RUNTIME_ID}.tar.gz.litelauncher-download"
}

terminate_process_tree() {
    local pid="$1"
    kill -TERM "$pid" >/dev/null 2>&1 || true
    if command -v pkill >/dev/null 2>&1; then
        pkill -TERM -P "$pid" >/dev/null 2>&1 || true
    fi
    sleep 0.2
    kill -KILL "$pid" >/dev/null 2>&1 || true
    if command -v pkill >/dev/null 2>&1; then
        pkill -KILL -P "$pid" >/dev/null 2>&1 || true
    fi
    wait "$pid" >/dev/null 2>&1 || true
}

request_manifest() {
    local destination="$1"
    local last_error=''
    local attempt temporary error_file header_file status_file pid fetch_code elapsed content_type body_size preview
    temporary="${destination}.download"
    error_file="${destination}.download-error"
    header_file="${destination}.headers"
    status_file="${destination}.status"

    for attempt in 1 2 3; do
        remove_path_quietly "$temporary"
        remove_path_quietly "$error_file"
        remove_path_quietly "$header_file"
        remove_path_quietly "$status_file"
        write_log 'INFO' "Java manifest request attempt ${attempt}: ${MANIFEST_URL}"

        (
            set +e
            if [[ "$DOWNLOADER" == 'curl' ]]; then
                curl --fail --location --silent --show-error \
                    --connect-timeout 15 --max-time 35 \
                    --header 'Accept: application/json' \
                    --user-agent 'LiteLauncher' \
                    --dump-header "$header_file" \
                    --output "$temporary" \
                    "$MANIFEST_URL" 2>"$error_file"
            else
                wget --quiet --server-response --timeout=15 --tries=1 \
                    --max-redirect=20 \
                    --header='Accept: application/json' \
                    --user-agent='LiteLauncher' \
                    --output-document="$temporary" \
                    "$MANIFEST_URL" 2>"$error_file"
            fi
            printf '%s\n' "$?" > "$status_file"
        ) &
        pid=$!
        elapsed=0

        while [[ ! -s "$status_file" ]]; do
            if (( elapsed >= 40 )); then
                terminate_process_tree "$pid"
                printf '124\n' > "$status_file"
                printf '%s\n' 'Java manifest request exceeded the 40-second watchdog.' > "$error_file"
                break
            fi
            write_progress 40 "${MSG_LOADING_JAVA} (${elapsed}s)" "$COLOR_CYAN"
            sleep 1
            elapsed=$((elapsed + 1))
        done

        fetch_code="$(cat "$status_file" 2>/dev/null || printf '1')"
        wait "$pid" >/dev/null 2>&1 || true
        [[ "$fetch_code" =~ ^[0-9]+$ ]] || fetch_code=1
        content_type="$(awk 'tolower($0) ~ /^content-type:/ {line=$0} END{sub(/^[^:]*:[[:space:]]*/, "", line); sub(/\r$/, "", line); print line}' "$header_file" 2>/dev/null || true)"
        body_size=0
        if [[ -f "$temporary" ]]; then
            body_size="$(file_size "$temporary" 2>/dev/null || printf '0')"
            [[ "$body_size" =~ ^[0-9]+$ ]] || body_size=0
        fi
        write_log 'INFO' "Java manifest response: downloader=${DOWNLOADER}; exit=${fetch_code}; bytes=${body_size}; content-type=${content_type:-unknown}"

        if (( fetch_code == 0 )) && (( body_size > 0 )); then
            mv -f "$temporary" "$destination"
            remove_path_quietly "$error_file"
            remove_path_quietly "$header_file"
            remove_path_quietly "$status_file"
            write_log 'INFO' "Java manifest loaded on attempt ${attempt}."
            return 0
        fi

        last_error="$(cat "$error_file" 2>/dev/null || true)"
        [[ -n "$last_error" ]] || last_error="${DOWNLOADER} exited with code ${fetch_code}."
        if (( body_size > 0 )); then
            preview="$(LC_ALL=C head -c 160 "$temporary" 2>/dev/null | tr '\r\n\t' '   ' || true)"
            write_log 'ERROR' "Java manifest response preview: ${preview}"
        fi
        write_log 'ERROR' "Java manifest request attempt ${attempt} failed: ${last_error}"
        remove_path_quietly "$temporary"
        remove_path_quietly "$error_file"
        remove_path_quietly "$header_file"
        remove_path_quietly "$status_file"
        if (( attempt < 3 )); then
            write_progress 40 "${MSG_LOADING_JAVA} retrying..." "$COLOR_YELLOW"
            sleep "$attempt"
        fi
    done

    die "Unable to load Java runtime manifest. ${last_error}"
}

parse_java_manifest() {
    local manifest="$1"
    local architecture="$2"
    local parser_error parser_output line_count
    parser_error="${manifest}.parser-error"
    remove_path_quietly "$parser_error"

    case "$JSON_PARSER" in
        python3|python)
            if ! parser_output="$($JSON_PARSER - "$manifest" "$architecture" 2>"$parser_error" <<'PY_EOF'
import json, math, re, sys

def fail(message):
    raise ValueError(message)

def obj(value, name):
    if not isinstance(value, dict): fail(f"Invalid Java manifest field: {name}.")
    return value

def text(value, name):
    if not isinstance(value, str) or not value.strip(): fail(f"Invalid Java manifest field: {name}.")
    if any(c in value for c in "\r\n\t"): fail(f"Invalid control character in Java manifest field: {name}.")
    return value

def integer(value, name):
    if isinstance(value, bool) or not isinstance(value, int): fail(f"Invalid Java manifest field: {name}.")
    return value

try:
    with open(sys.argv[1], 'r', encoding='utf-8-sig') as fh:
        root = json.load(fh)
    root = obj(root, 'Java manifest root')
    if integer(root.get('schemaVersion'), 'schemaVersion') != 1:
        fail('Unsupported Java manifest schema version.')
    runtime = obj(obj(obj(obj(root.get('runtimes'), 'runtimes').get('25'), 'runtimes.25').get('linux'), 'runtimes.25.linux').get(sys.argv[2]), f'linux/{sys.argv[2]}')
    name = text(runtime.get('name'), 'name')
    url = text(runtime.get('url'), 'url')
    sha1 = text(runtime.get('sha1'), 'sha1')
    size = integer(runtime.get('size'), 'size')
    if '/' in name or '\\' in name: fail('Invalid Java package name in manifest.')
    lower = name.lower()
    if not (lower.endswith('.zip') or lower.endswith('.tar.gz')): fail('Unsupported Java archive format in manifest.')
    if not re.fullmatch(r'[0-9a-fA-F]{40}', sha1): fail('Invalid Java package SHA-1 in manifest.')
    if size <= 0 or size > 9007199254740991: fail('Invalid Java package size in manifest.')
    print(name); print(url); print(sha1.lower()); print(size)
except json.JSONDecodeError as exc:
    print(f'Invalid Java runtime manifest JSON: {exc}', file=sys.stderr)
    sys.exit(1)
except Exception as exc:
    print(str(exc), file=sys.stderr)
    sys.exit(1)
PY_EOF
)"; then
                LAST_ERROR="$(tail -n 1 "$parser_error" 2>/dev/null || true)"
                [[ -n "$LAST_ERROR" ]] || LAST_ERROR='Unable to parse the Java runtime manifest.'
                write_log 'ERROR' "Java manifest parser failed: ${LAST_ERROR}"
                remove_path_quietly "$parser_error"
                return 1
            fi
            ;;
        perl)
            if ! parser_output="$(perl -MJSON::PP -0777 - "$manifest" "$architecture" 2>"$parser_error" <<'PERL_EOF'
use strict; use warnings; use utf8;
sub fail { die $_[0] . "\n"; }
sub obj { ref($_[0]) eq 'HASH' or fail("Invalid Java manifest field: $_[1]."); return $_[0]; }
sub txt { defined($_[0]) && !ref($_[0]) && $_[0] =~ /\S/ or fail("Invalid Java manifest field: $_[1]."); $_[0] !~ /[\r\n\t]/ or fail("Invalid control character in Java manifest field: $_[1]."); return $_[0]; }
my ($path,$arch)=@ARGV; open my $fh,'<:raw',$path or fail('Unable to read the downloaded manifest.'); local $/; my $raw=<$fh>; $raw =~ s/^\xEF\xBB\xBF//;
my $root; eval { $root=JSON::PP->new->utf8->decode($raw); 1 } or fail('Invalid Java runtime manifest JSON.');
$root=obj($root,'Java manifest root'); defined($root->{schemaVersion}) && $root->{schemaVersion} =~ /^1$/ or fail('Unsupported Java manifest schema version.');
my $rt=obj(obj(obj(obj($root->{runtimes},'runtimes')->{'25'},'runtimes.25')->{linux},'runtimes.25.linux')->{$arch},"linux/$arch");
my $name=txt($rt->{name},'name'); my $url=txt($rt->{url},'url'); my $sha=txt($rt->{sha1},'sha1'); my $size=$rt->{size};
$name !~ m{[\\/]} or fail('Invalid Java package name in manifest.'); lc($name) =~ /(?:\.zip|\.tar\.gz)$/ or fail('Unsupported Java archive format in manifest.');
$sha =~ /^[0-9a-fA-F]{40}$/ or fail('Invalid Java package SHA-1 in manifest.'); defined($size) && !ref($size) && $size =~ /^\d+$/ && $size > 0 or fail('Invalid Java package size in manifest.');
print "$name\n$url\n",lc($sha),"\n$size\n";
PERL_EOF
)"; then
                LAST_ERROR="$(tail -n 1 "$parser_error" 2>/dev/null || true)"
                [[ -n "$LAST_ERROR" ]] || LAST_ERROR='Unable to parse the Java runtime manifest.'
                write_log 'ERROR' "Java manifest parser failed: ${LAST_ERROR}"
                remove_path_quietly "$parser_error"
                return 1
            fi
            ;;
        jq)
            if ! parser_output="$(jq -er --arg arch "$architecture" '
                if .schemaVersion != 1 then error("Unsupported Java manifest schema version.") else . end
                | .runtimes["25"].linux[$arch] as $r
                | if ($r|type) != "object" then error("Java 25 is unavailable for linux/"+$arch+".") else $r end
                | [.name,.url,.sha1,(.size|tostring)]
                | if any(.[]; type != "string" or length == 0) then error("Invalid Java manifest fields.") else . end
                | .[]' "$manifest" 2>"$parser_error")"; then
                LAST_ERROR="$(tail -n 1 "$parser_error" 2>/dev/null || true)"
                [[ -n "$LAST_ERROR" ]] || LAST_ERROR='Unable to parse the Java runtime manifest.'
                write_log 'ERROR' "Java manifest parser failed: ${LAST_ERROR}"
                remove_path_quietly "$parser_error"
                return 1
            fi
            ;;
    esac
    remove_path_quietly "$parser_error"

    line_count="$(printf '%s\n' "$parser_output" | awk 'END { print NR }')"
    if [[ "$line_count" != '4' ]]; then
        LAST_ERROR="Java manifest parser returned ${line_count} fields instead of 4."
        write_log 'ERROR' "$LAST_ERROR"
        return 1
    fi

    PACKAGE_NAME="$(printf '%s\n' "$parser_output" | sed -n '1p')"
    PACKAGE_URL="$(printf '%s\n' "$parser_output" | sed -n '2p')"
    PACKAGE_SHA1="$(printf '%s\n' "$parser_output" | sed -n '3p')"
    PACKAGE_SIZE="$(printf '%s\n' "$parser_output" | sed -n '4p')"
    return 0
}

resolve_java_package() {
    local architecture="$1"
    local manifest_json package_name_lower preview
    manifest_json="${JAVA_DIRECTORY}/.litelauncher-java-manifest.json"
    remove_path_quietly "$manifest_json"

    request_manifest "$manifest_json"
    if ! parse_java_manifest "$manifest_json" "$architecture"; then
        preview="$(LC_ALL=C head -c 200 "$manifest_json" 2>/dev/null | tr '\r\n\t' '   ' || true)"
        write_log 'ERROR' "Invalid manifest preview: ${preview}"
        remove_path_quietly "$manifest_json"
        die "$LAST_ERROR"
    fi
    remove_path_quietly "$manifest_json"

    [[ -n "$PACKAGE_NAME" && "$PACKAGE_NAME" != *'/'* && "$PACKAGE_NAME" != *'\\'* ]] || die 'Invalid Java package name in manifest.'
    package_name_lower="$(printf '%s' "$PACKAGE_NAME" | tr '[:upper:]' '[:lower:]')"
    case "$package_name_lower" in
        *.zip) PACKAGE_EXTENSION='.zip' ;;
        *.tar.gz) PACKAGE_EXTENSION='.tar.gz' ;;
        *) die 'Unsupported Java archive format in manifest.' ;;
    esac
    [[ -n "$PACKAGE_URL" ]] || die 'Invalid Java package URL in manifest.'
    [[ "$PACKAGE_SHA1" =~ ^[0-9a-fA-F]{40}$ ]] || die 'Invalid Java package SHA-1 in manifest.'
    [[ "$PACKAGE_SIZE" =~ ^[0-9]+$ ]] && (( PACKAGE_SIZE > 0 )) || die 'Invalid Java package size in manifest.'
    PACKAGE_SHA1="$(printf '%s' "$PACKAGE_SHA1" | tr '[:upper:]' '[:lower:]')"
    write_log 'INFO' "Resolved Java package: name=${PACKAGE_NAME}; bytes=${PACKAGE_SIZE}; sha1=${PACKAGE_SHA1}; url=${PACKAGE_URL}"
}

test_java_archive() {
    local path="$1"
    local expected_size="$2"
    local expected_sha1="$3"
    local actual_size actual_sha1
    [[ -f "$path" ]] || return 1
    actual_size="$(file_size "$path")"
    [[ "$actual_size" == "$expected_size" ]] || return 1
    actual_sha1="$(sha1_file "$path")"
    [[ "$actual_sha1" == "$expected_sha1" ]]
}

download_java_archive() {
    local archive="$1"
    local temporary="${archive}.litelauncher-download"
    local error_file="${archive}.download-error"
    local status_file="${archive}.download-status"
    local attempt pid downloaded overall_tenths overall download_percent fetch_code last_error='' start_seconds now_seconds elapsed

    for attempt in 1 2 3; do
        remove_path_quietly "$temporary"
        remove_path_quietly "$error_file"
        remove_path_quietly "$status_file"
        write_log 'INFO' "Download attempt ${attempt} for Java: ${PACKAGE_URL}"

        (
            set +e
            if [[ "$DOWNLOADER" == 'curl' ]]; then
                curl --fail --location --silent --show-error \
                    --connect-timeout 30 --max-time 1800 \
                    --speed-time 180 --speed-limit 1 \
                    --user-agent 'LiteLauncher' \
                    --output "$temporary" \
                    "$PACKAGE_URL" 2>"$error_file"
            else
                wget --quiet --timeout=30 --read-timeout=180 --tries=1 \
                    --max-redirect=20 --user-agent='LiteLauncher' \
                    --output-document="$temporary" \
                    "$PACKAGE_URL" 2>"$error_file"
            fi
            printf '%s\n' "$?" > "$status_file"
        ) &
        pid=$!
        start_seconds="$(date +%s)"

        while [[ ! -s "$status_file" ]]; do
            now_seconds="$(date +%s)"
            elapsed=$((now_seconds - start_seconds))
            if (( elapsed >= 1810 )); then
                terminate_process_tree "$pid"
                printf '124\n' > "$status_file"
                printf '%s\n' 'Java download exceeded the 30-minute watchdog.' > "$error_file"
                break
            fi
            downloaded=0
            if [[ -f "$temporary" ]]; then
                downloaded="$(file_size "$temporary" 2>/dev/null || printf '0')"
                [[ "$downloaded" =~ ^[0-9]+$ ]] || downloaded=0
            fi
            (( downloaded > PACKAGE_SIZE )) && downloaded=$PACKAGE_SIZE
            download_percent=$((downloaded * 100 / PACKAGE_SIZE))
            overall_tenths=$((420 + 258 * downloaded / PACKAGE_SIZE))
            overall=$(((overall_tenths + 5) / 10))
            write_progress "$overall" "${MSG_DOWNLOADING_JAVA} ${download_percent}%" "$COLOR_CYAN"
            sleep 0.20
        done

        fetch_code="$(cat "$status_file" 2>/dev/null || printf '1')"
        wait "$pid" >/dev/null 2>&1 || true
        [[ "$fetch_code" =~ ^[0-9]+$ ]] || fetch_code=1
        remove_path_quietly "$status_file"

        if (( fetch_code == 0 )) && test_java_archive "$temporary" "$PACKAGE_SIZE" "$PACKAGE_SHA1"; then
            mv -f "$temporary" "$archive"
            remove_path_quietly "$error_file"
            write_log 'INFO' "Downloaded and verified Java archive: ${archive}"
            write_progress 68 "${MSG_DOWNLOADING_JAVA} 100%" "$COLOR_CYAN"
            return 0
        fi

        last_error="$(cat "$error_file" 2>/dev/null || true)"
        if (( fetch_code == 0 )); then
            last_error='Downloaded Java archive failed size or SHA-1 verification.'
        elif [[ -z "$last_error" ]]; then
            last_error="${DOWNLOADER} exited with code ${fetch_code}."
        fi
        write_log 'ERROR' "Java download attempt ${attempt} failed: ${last_error}"
        remove_path_quietly "$temporary"
        remove_path_quietly "$error_file"
        (( attempt < 3 )) && sleep "$attempt"
    done

    die "Unable to download Java runtime. ${last_error}"
}

verify_fresh_java() {
    local java_executable="$1"
    local error_file status_file pid exit_code elapsed
    error_file="${JAVA_DIRECTORY}/.litelauncher-java-check.$$.error"
    status_file="${JAVA_DIRECTORY}/.litelauncher-java-check.$$.status"
    remove_path_quietly "$error_file"
    remove_path_quietly "$status_file"

    (
        set +e
        "$java_executable" -version >/dev/null 2>"$error_file"
        printf '%s\n' "$?" > "$status_file"
    ) &
    pid=$!
    elapsed=0
    while [[ ! -s "$status_file" ]]; do
        if (( elapsed >= 20 )); then
            terminate_process_tree "$pid"
            remove_path_quietly "$error_file"
            remove_path_quietly "$status_file"
            die 'The installed Java runtime did not start within 20 seconds.'
            return 1
        fi
        write_progress 72 "${MSG_INSTALLING_JAVA} ${MSG_VERIFYING_JAVA} (${elapsed}s)" "$COLOR_YELLOW"
        sleep 1
        elapsed=$((elapsed + 1))
    done

    exit_code="$(cat "$status_file" 2>/dev/null || printf '1')"
    wait "$pid" >/dev/null 2>&1 || true
    remove_path_quietly "$status_file"
    if [[ ! "$exit_code" =~ ^[0-9]+$ ]] || (( exit_code != 0 )); then
        LAST_ERROR="$(cat "$error_file" 2>/dev/null | tail -n 1 || true)"
        [[ -n "$LAST_ERROR" ]] || LAST_ERROR="Java exited with code ${exit_code}."
        write_log 'ERROR' "Fresh Java verification failed: ${LAST_ERROR}"
        remove_path_quietly "$error_file"
        return 1
    fi
    write_log 'INFO' "Fresh Java verification succeeded: ${java_executable}"
    remove_path_quietly "$error_file"
    return 0
}

archive_paths_are_safe() {
    local archive="$1"
    local extension="$2"
    local listing="${archive}.entries"
    local entry
    remove_path_quietly "$listing"

    if [[ "$extension" == '.tar.gz' ]]; then
        if ! tar -tzf "$archive" > "$listing" 2>/dev/null; then
            remove_path_quietly "$listing"
            return 3
        fi
    else
        command -v unzip >/dev/null 2>&1 || return 2
        if ! unzip -Z1 "$archive" > "$listing" 2>/dev/null; then
            remove_path_quietly "$listing"
            return 3
        fi
    fi

    while IFS= read -r entry; do
        [[ -n "$entry" ]] || continue
        case "$entry" in
            /*|../*|*/../*|*/..)
                remove_path_quietly "$listing"
                return 1
                ;;
        esac
    done < "$listing"
    remove_path_quietly "$listing"
    return 0
}

extract_java_archive() {
    local archive="$1"
    local staging safe_code
    local -a entries
    remove_path_quietly "$JAVA_TEMP_ROOT"
    mkdir -p "$JAVA_TEMP_ROOT"

    if archive_paths_are_safe "$archive" "$PACKAGE_EXTENSION"; then
        safe_code=0
    else
        safe_code=$?
    fi
    if (( safe_code != 0 )); then
        cleanup_java_temporary_files "$archive"
        case "$safe_code" in
            2) die 'The unzip utility is required for the Java package supplied by the server.' ;;
            3) die 'Downloaded Java archive is unreadable.' ;;
            *) die 'Downloaded Java archive contains unsafe paths.' ;;
        esac
        return 1
    fi

    if [[ "$PACKAGE_EXTENSION" == '.tar.gz' ]]; then
        if ! tar -xzf "$archive" -C "$JAVA_TEMP_ROOT" --strip-components=1; then
            cleanup_java_temporary_files "$archive"
            die 'Java runtime extraction failed.'
        fi
        return 0
    fi

    staging="${JAVA_TEMP_ROOT}.zip-staging"
    remove_path_quietly "$staging"
    mkdir -p "$staging"
    if ! unzip -q "$archive" -d "$staging"; then
        remove_path_quietly "$staging"
        cleanup_java_temporary_files "$archive"
        die 'Java runtime ZIP extraction failed.'
    fi

    shopt -s nullglob dotglob
    entries=("$staging"/*)
    shopt -u nullglob dotglob
    if (( ${#entries[@]} == 1 )) && [[ -d "${entries[0]}" ]]; then
        cp -a "${entries[0]}"/. "$JAVA_TEMP_ROOT"/
    else
        cp -a "$staging"/. "$JAVA_TEMP_ROOT"/
    fi
    remove_path_quietly "$staging"
}

install_java_runtime() {
    local architecture="$1"
    local archive temporary_java installed_java

    write_progress 36 "$MSG_PREPARING_JAVA" "$COLOR_CYAN"
    cleanup_java_temporary_files ''
    write_progress 38 "$MSG_CHECKING_JAVA" "$COLOR_CYAN"

    if java_runtime_entry_exists; then
        die 'An unexpected Java runtime exists after clean installation preparation.'
    fi

    write_progress 39 "${MSG_CHECKING_JAVA} ${MSG_CHECKING_NOT_FOUND}" "$COLOR_CYAN"
    write_progress 40 "$MSG_LOADING_JAVA" "$COLOR_CYAN"
    write_log 'INFO' "Loading Java runtime manifest: ${MANIFEST_URL}"
    resolve_java_package "$architecture"
    archive="${JAVA_DIRECTORY}/${RUNTIME_ID}${PACKAGE_EXTENSION}"
    cleanup_java_temporary_files "$archive"

    write_log 'INFO' "Installing Java runtime: ${RUNTIME_ID} -> ${JAVA_ROOT}"
    remove_path_quietly "$JAVA_ROOT"

    download_java_archive "$archive"
    write_stage 70 "$MSG_INSTALLING_JAVA" "$COLOR_YELLOW"
    write_log 'INFO' "Extracting Java archive: ${archive} -> ${JAVA_TEMP_ROOT}"
    extract_java_archive "$archive"

    if ! temporary_java="$(find_java "$JAVA_TEMP_ROOT")"; then
        cleanup_java_temporary_files "$archive"
        die 'Java executable was not found after extraction.'
    fi
    chmod 755 "$temporary_java" >/dev/null 2>&1 || true
    if ! verify_fresh_java "$temporary_java"; then
        cleanup_java_temporary_files "$archive"
        die "$LAST_ERROR"
    fi

    remove_path_quietly "$JAVA_ROOT"
    if ! mv "$JAVA_TEMP_ROOT" "$JAVA_ROOT"; then
        cleanup_java_temporary_files "$archive"
        die 'Java runtime installation failed while moving extracted files.'
    fi
    remove_path_quietly "$archive"

    if ! installed_java="$(find_java "$JAVA_ROOT")"; then
        cleanup_java_temporary_files "$archive"
        die 'Java executable was not found after installation.'
    fi
    chmod 755 "$installed_java" >/dev/null 2>&1 || true

    write_log 'INFO' "Java runtime installed: ${RUNTIME_ID} -> ${installed_java}"
    write_stage 75 "$MSG_JAVA_READY" "$COLOR_GREEN"
    JAVA_EXECUTABLE="$installed_java"
}

create_unix_launcher_script() {
    local temporary="${UNIX_SCRIPT}.litelauncher-install"
    cat > "$temporary" <<'SCRIPT_EOF'
#!/usr/bin/env bash
set -e
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
JAVA_ROOT="$APP_HOME/../java/jre-25"
JAVA_EXE="$JAVA_ROOT/bin/java"
if [ ! -x "$JAVA_EXE" ] && [ -x "$JAVA_ROOT/Contents/Home/bin/java" ]; then
  JAVA_EXE="$JAVA_ROOT/Contents/Home/bin/java"
fi
if [ ! -x "$JAVA_EXE" ]; then
  echo "LiteLauncher Java 25 was not found. Reinstall LiteLauncher and try again."
  exit 1
fi
cd "$APP_HOME"
exec "$JAVA_EXE" -jar "$APP_HOME/Bootstrap.jar"
SCRIPT_EOF
    chmod 755 "$temporary"
    mv -f "$temporary" "$UNIX_SCRIPT"
    write_log 'INFO' "Launch script created: ${UNIX_SCRIPT}"
}

desktop_exec_arg() {
    local value="$1"
    value=${value//\\/\\\\}
    value=${value//\"/\\\"}
    value=${value//\$/\\$}
    value=${value//\`/\\\`}
    printf '"%s"' "$value"
}

desktop_value() {
    local value="$1"
    value=${value//$'\n'/}
    value=${value//$'\r'/}
    printf '%s' "$value"
}

refresh_linux_desktop_caches() {
    if command -v xdg-desktop-menu >/dev/null 2>&1; then
        xdg-desktop-menu forceupdate >/dev/null 2>&1 || write_log 'INFO' 'xdg-desktop-menu cache refresh failed or was unsupported.'
    fi
    if command -v update-desktop-database >/dev/null 2>&1; then
        update-desktop-database "$APPLICATIONS_DIRECTORY" >/dev/null 2>&1 || write_log 'INFO' 'update-desktop-database failed or was unsupported.'
    fi
    if command -v gtk-update-icon-cache >/dev/null 2>&1; then
        gtk-update-icon-cache -q "$HICOLOR_DIRECTORY" >/dev/null 2>&1 || write_log 'INFO' 'gtk-update-icon-cache failed or was unsupported.'
    fi
}

create_local_linux_shortcut() {
    local temporary="${LOCAL_DESKTOP_FILE}.litelauncher-install"
    local exec_value path_value icon_value
    exec_value="$(desktop_exec_arg "$UNIX_SCRIPT")"
    path_value="$(desktop_value "$BOOTSTRAP_DIRECTORY")"
    icon_value="$(desktop_value "$ICON_FILE")"

    rm -rf "$temporary" >/dev/null 2>&1 || true
    cat > "$temporary" <<DESKTOP_EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=LiteLauncher
Comment=LiteLauncher
Exec=${exec_value}
Path=${path_value}
Icon=${icon_value}
Terminal=false
Categories=Game;
StartupNotify=true
StartupWMClass=net-litelauncher-LiteLauncher
DESKTOP_EOF
    chmod 755 "$temporary"
    mv -f "$temporary" "$LOCAL_DESKTOP_FILE"

    [[ -x "$LOCAL_DESKTOP_FILE" ]] || die 'Generated Linux shortcut is not executable.'
    if command -v gio >/dev/null 2>&1; then
        gio set "$LOCAL_DESKTOP_FILE" metadata::trusted true >/dev/null 2>&1 || true
    fi
    write_log 'INFO' "Local Linux shortcut created: ${LOCAL_DESKTOP_FILE}"
}

create_local_shortcut_only() {
    [[ -f "$BOOTSTRAP_JAR" ]] || die 'Installed LiteLauncher Bootstrap.jar was not found. Reinstall LiteLauncher and try again.'
    [[ -f "$ICON_FILE" ]] || die 'Installed LiteLauncher shortcut icon was not found. Reinstall LiteLauncher and try again.'
    [[ -x "$UNIX_SCRIPT" ]] || die 'Installed LiteLauncher launch script was not found. Reinstall LiteLauncher and try again.'
    if [[ ! -x "${JAVA_ROOT}/bin/java" && ! -x "${JAVA_ROOT}/Contents/Home/bin/java" ]]; then
        die 'Installed LiteLauncher Java 25 was not found. Reinstall LiteLauncher and try again.'
    fi

    write_stage 40 "$MSG_CREATING_SHORTCUT"
    create_local_linux_shortcut
    write_stage 100 "$MSG_DONE" "$COLOR_GREEN"
    complete_progress_line
    printf '\n'
    printf '%s  %s%s\n' "$COLOR_GREEN" "$MSG_SHORTCUT_CREATED_SUCCESSFULLY" "$COLOR_RESET"
    printf '%s  %s%s%s\n' "$COLOR_DARK_GRAY" "$MSG_SHORTCUT_LABEL" "$LOCAL_DESKTOP_FILE" "$COLOR_RESET"
    write_log 'INFO' "Local shortcut creation completed successfully: ${LOCAL_DESKTOP_FILE}"
    INSTALLER_FINISHED=1
    sleep 0.65
    finish_terminal_session
}

create_linux_application_shortcut() {
    local temporary="${DESKTOP_FILE}.litelauncher-install"
    local exec_value path_value
    exec_value="$(desktop_exec_arg "$UNIX_SCRIPT")"
    path_value="$(desktop_value "$BOOTSTRAP_DIRECTORY")"

    mkdir -p "$APPLICATIONS_DIRECTORY" "$(dirname "$APPLICATION_ICON")"
    cp -f "$ICON_FILE" "$APPLICATION_ICON"
    chmod 644 "$APPLICATION_ICON" >/dev/null 2>&1 || true

    cat > "$temporary" <<DESKTOP_EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=LiteLauncher
Comment=LiteLauncher
Exec=${exec_value}
Path=${path_value}
Icon=net.litelauncher.LiteLauncher
Terminal=false
Categories=Game;
StartupNotify=true
StartupWMClass=net-litelauncher-LiteLauncher
DESKTOP_EOF
    chmod 755 "$temporary"
    mv -f "$temporary" "$DESKTOP_FILE"

    [[ -x "$DESKTOP_FILE" ]] || die 'Generated Linux application shortcut is not executable.'
    [[ -f "$APPLICATION_ICON" ]] || die 'Generated Linux application icon is missing.'
    refresh_linux_desktop_caches
    write_log 'INFO' "Linux application shortcut created: ${DESKTOP_FILE}"
    write_log 'INFO' "Linux application icon created: ${APPLICATION_ICON}"
}

start_litelauncher() {
    nohup "$UNIX_SCRIPT" >/dev/null 2>&1 &
    write_log 'INFO' "LiteLauncher started: ${UNIX_SCRIPT}; pid=$!"
}

open_shortcut_folder() {
    mkdir -p "$APPLICATIONS_DIRECTORY"
    if command -v xdg-open >/dev/null 2>&1; then xdg-open "$APPLICATIONS_DIRECTORY" >/dev/null 2>&1 &
    elif command -v gio >/dev/null 2>&1; then gio open "$APPLICATIONS_DIRECTORY" >/dev/null 2>&1 &
    else die 'No supported desktop folder opener (xdg-open or gio) is available.'; return 1; fi
    write_log 'INFO' "Shortcut folder opened: ${APPLICATIONS_DIRECTORY}"
}

open_shortcut_folder_only() {
    printf '\n'
    printf '%s  %s%s\n' "$COLOR_CYAN" "$MSG_OPENING_SHORTCUT_FOLDER" "$COLOR_RESET"
    open_shortcut_folder
    INSTALLER_FINISHED=1
    sleep 0.35
    finish_terminal_session
}

show_post_install_menu() {
    local key=''
    printf '\n'
    printf '%s  %s%s\n' "$COLOR_GREEN" "$MSG_INSTALLED_SUCCESSFULLY" "$COLOR_RESET"
    printf '%s  %s%s\n' "$COLOR_YELLOW" "$MSG_POST_INSTALL_PROMPT" "$COLOR_RESET"
    printf '\n'
    printf '%s  [1] %s%s\n' "$COLOR_CYAN" "$MSG_OPEN_LAUNCHER" "$COLOR_RESET"
    printf '%s      %s%s\n' "$COLOR_DARK_GRAY" "$MSG_OPEN_LAUNCHER_DESC" "$COLOR_RESET"
    printf '%s  [2] %s%s\n' "$COLOR_CYAN" "$MSG_OPEN_SHORTCUT_FOLDER" "$COLOR_RESET"
    printf '%s      %s%s\n' "$COLOR_DARK_GRAY" "$MSG_POST_OPEN_FOLDER_DESC" "$COLOR_RESET"
    printf '%s  [3] %s%s\n' "$COLOR_CYAN" "$MSG_EXIT_INSTALLER" "$COLOR_RESET"
    printf '%s      %s%s\n' "$COLOR_DARK_GRAY" "$MSG_EXIT_DESC" "$COLOR_RESET"
    printf '\n'
    INSTALLER_FINISHED=1
    [[ -t 0 ]] || return 0
    while true; do
        printf '%s  %s%s' "$COLOR_YELLOW" "$MSG_PRESS_POST" "$COLOR_RESET"
        IFS= read -r -n 1 key || true
        printf '%s\n' "$key"
        case "$key" in
            1)
                printf '%s  %s%s\n' "$COLOR_GREEN" "$MSG_STARTING_LAUNCHER" "$COLOR_RESET"
                if start_litelauncher; then
                    break
                fi
                write_log 'ERROR' 'Post-install launcher start failed.'
                printf '%s  %s%s\n' "$COLOR_RED" "$MSG_ACTION_FAILED" "$COLOR_RESET"
                ;;
            2)
                printf '%s  %s%s\n' "$COLOR_CYAN" "$MSG_OPENING_SHORTCUT_FOLDER" "$COLOR_RESET"
                if open_shortcut_folder; then
                    break
                fi
                write_log 'ERROR' 'Post-install shortcut folder open failed.'
                printf '%s  %s%s\n' "$COLOR_RED" "$MSG_ACTION_FAILED" "$COLOR_RESET"
                ;;
            3)
                break
                ;;
        esac
    done
    sleep 0.35
    finish_terminal_session
}

finish_terminal_session() {
    return 0
}

show_installation_failure() {
    local message="$1"
    local exit_code="${2:-1}"
    local line="${3:-unknown}"
    local command="${4:-unknown}"
    local key=''

    complete_progress_line
    write_log 'ERROR' "Installation failed. Error: ${message}; exit=${exit_code}; line=${line}; command=${command}"
    printf '\n'
    printf '%s  %s%s\n' "$COLOR_RED" "$MSG_INSTALLATION_FAILED" "$COLOR_RESET"
    printf '%s  %s%s%s\n' "$COLOR_RED" "$MSG_ERROR_LABEL" "$message" "$COLOR_RESET"
    if [[ -n "$LOG_PATH" ]]; then
        printf '%s  %s%s%s\n' "$COLOR_DARK_GRAY" "$MSG_LOG_LABEL" "$LOG_PATH" "$COLOR_RESET"
    fi
    printf '\n'
    printf '%s  %s%s\n' "$COLOR_YELLOW" "$MSG_PRESS_LOG" "$COLOR_RESET"

    if [[ -t 0 ]]; then
        IFS= read -r -n 1 key || true
        printf '\n'
        if [[ "$key" == 'l' || "$key" == 'L' ]]; then
            if command -v xdg-open >/dev/null 2>&1; then
                xdg-open "$LOG_PATH" >/dev/null 2>&1 || true
            elif command -v gio >/dev/null 2>&1; then
                gio open "$LOG_PATH" >/dev/null 2>&1 || true
            fi
        fi
    fi
}

on_error() {
    local exit_code="$1"
    local line="$2"
    local command="$3"
    local message

    trap - ERR
    set +e
    (( INSTALLER_FINISHED == 0 )) || exit "$exit_code"
    message="$LAST_ERROR"
    if [[ -z "$message" ]]; then
        message="Command failed at line ${line}: ${command}"
    fi
    show_installation_failure "$message" "$exit_code" "$line" "$command"
    exit 10
}

install_litelauncher() {
    write_stage 5 "$MSG_PREPARING_FILES"
    ensure_directories

    write_stage 20 "$MSG_WRITING_BOOTSTRAP"
    write_bootstrap_payload "$BOOTSTRAP_JAR"
    write_log 'INFO' "Bootstrap installed: ${BOOTSTRAP_JAR}"

    write_stage 30 "$MSG_WRITING_ICONS"
    write_icon_payload "$ICON_FILE"
    write_log 'INFO' "Shortcut icon installed: ${ICON_FILE}"

    write_stage 35 "$MSG_PREPARING_JAVA"
    install_java_runtime "$ARCHITECTURE"

    write_stage 80 "$MSG_WRITING_LAUNCHER"
    create_unix_launcher_script

    write_stage 90 "$MSG_CREATING_SHORTCUT"
    create_linux_application_shortcut

    write_stage 100 "$MSG_DONE" "$COLOR_GREEN"
    complete_progress_line
    write_log 'INFO' 'Installation completed successfully.'
    show_post_install_menu
}

run_installer() {
    local action

    write_banner
    get_paths
    action="$(read_installer_action)"

    if [[ "$action" == 'exit' ]]; then
        printf '\n'
        printf '%s  %s%s\n' "$COLOR_DARK_GRAY" "$MSG_NO_CHANGES" "$COLOR_RESET"
        INSTALLER_FINISHED=1
        sleep 0.35
        return 0
    fi

    initialize_operation_log "$action"

    if [[ "$action" == 'uninstall' ]]; then
        uninstall_litelauncher
        return 0
    fi

    if [[ "$action" == 'create-local-shortcut' ]]; then
        create_local_shortcut_only
        return 0
    fi

    if [[ "$action" == 'open-shortcut-folder' ]]; then
        open_shortcut_folder_only
        return 0
    fi

    ensure_required_tools
    get_architecture
    write_log 'INFO' "Java manifest architecture: ${ARCHITECTURE}"

    if [[ "$action" == 'reinstall' ]]; then
        prepare_full_reinstallation
    fi

    install_litelauncher
}

trap 'on_error "$?" "$LINENO" "$BASH_COMMAND"' ERR
run_installer
exit 0
