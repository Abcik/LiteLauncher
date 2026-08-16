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
bmNoZXIvdWkvUGl4ZWxUZXh0JEFsaWdubWVudC5jbGFzc41TbU/TUBR+7rqtay0wJ4Ig+IIoGyAV
BEU3CWQZgplA2FxC/HQ3rlDSdUnXEj76k2QkQjQaPvujjOdeJqIYsjbp03P6nOe83NMfP798B1BA
TkeEYcwTge06gXB56NV2hW+Hjr3hHAi3LA6C0SXX2fHqwgsMMEQtxBAnLBaWywyZYoexWRmcsGDI
4Hi+sFYubErXNQuWdMU2V1+vlKWn20IPkgz6aGWp+K5QYhh/33EWEync0JG00IubpFpzG55g6E1n
int8n9su93bs9eqeqEluP27pGLAwiNuU9w+h4IV1KmCfu6FY/8Awl74QnXd5s5m94CgFvuPtZC9m
kAJKf9jCHdyljnOO5wQLDH3py5GrmUoCMZN6H07ASMCSrw8tPMKYHIMqo8kwmc50PghK+Tsumm9s
0wx6io4n1sJ6VfhlXnXJ8+o/tXR+oAypYqPG3Qr3HanXFo16vC7kt0vSDMm3IthtbG9wnziB8GVx
wa5DYJRIlgehT6FaOlOhXcnV3PbMsv9MNtdxjQuU1Cw1Qr8mlh1ZXfc5a0pqUpo10QxWGs1Axwua
/5XCDNaq5wlfbYCcrHGeaXGajqyf/qno4oD8QQBCo42WxOSgXGtixMifxhjxM2SNEMrLbKHrM66f
IvWJLNp5esbVN078QUwgovizhNJrptj4CfpOwSQ/8hd/kp7WGQuPMUVoy7zk+CiptJDQlNg0oUao
j08MHWPo8AolHU9kF0rJiEB1kpQKMmQmacplbbdkI6pa6o9/A9vSWtBKW9EW9NJWrEWncbm/BJ5i
ph38ph089ZViU/e0Y9w/gqaMkagydGU8iCnDPMHoEboOzzVNKgeoUjGclM7Km8Wcwmd4rnAeLxWT
0T2P7OLAL1BLBwiTLz9UkQIAABAFAABQSwMEFAAICAgAp3X0XAAAAAAAAAAAAAAAACMAAABuZXQv
bGl0ZWxhdW5jaGVyL3VpL1BpeGVsVGV4dC5jbGFzc41XCVcTWRb+ilSoEB+KICoCgkpjErTpRqVH
cYsQIBoCkyBoLzJFUkBpkdCVikDP0rN29+z74qzOZs/iLDKSdpzpnp79nPkZ8zv6TM+9r4pNA90n
pN7Lfe+++93vLvX4z/8evgUghn8FUQGfBlXAj0oFNdf1m3qnpeemO4cnrxsZR0HlaTNnOmcV+ELh
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
aXRlbGF1bmNoZXIvYmFja2VuZC9Cb290c3RyYXBMb2cuY2xhc3NQSwECCgAKAAAIAACndfRcAAAA
AAAAAAAAAAAAIgAAAAAAAAAAAAAAAADsHQAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2Rvd25s
b2FkL1BLAQIUABQACAgIAKd19FxaPljQtgEAAFEDAAA5AAAAAAAAAAAAAAAAACweAABuZXQvbGl0
ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRFeGNlcHRpb24uY2xhc3NQSwECFAAU
AAgICACndfRcsPSLcf0DAACUCQAANAAAAAAAAAAAAAAAAABJIAAAbmV0L2xpdGVsYXVuY2hlci9i
YWNrZW5kL2Rvd25sb2FkL0Rvd25sb2FkRmlsZS5jbGFzc1BLAQIUABQACAgIAKd19FxycZjOzAAA
ABkBAAA4AAAAAAAAAAAAAAAAAKgkAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQv
RG93bmxvYWRQcm9ncmVzcy5jbGFzc1BLAQIUABQACAgIAKd19Fy+9o2LqQMAAHsJAABJAAAAAAAA
AAAAAAAAANolAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRTZXJ2
aWNlJFZhbGlkYXRpb25TdW1tYXJ5LmNsYXNzUEsBAhQAFAAICAgAp3X0XAXbgMucKAAA8VwAADcA
AAAAAAAAAAAAAAAA+ikAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9kb3dubG9hZC9Eb3dubG9h
ZFNlcnZpY2UuY2xhc3NQSwECCgAKAAAIAACndfRcAAAAAAAAAAAAAAAAHgAAAAAAAAAAAAAAAAD7
UgAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2phdmEvUEsBAhQAFAAICAgApnX0XKg4z784CAAA
+BEAADYAAAAAAAAAAAAAAAAAN1MAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9qYXZhL0phdmFS
dW50aW1lTG9jYXRvci5jbGFzc1BLAQIUABQACAgIAKd19FxzWE4jIwwAAPAXAABJAAAAAAAAAAAA
AAAAANNbAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvamF2YS9KYXZhUnVudGltZU1hbmlmZXN0
U2VydmljZSRKc29uUGFyc2VyLmNsYXNzUEsBAhQAFAAICAgAp3X0XFmnNV1SEAAA9SAAAD4AAAAA
AAAAAAAAAAAAbWgAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9qYXZhL0phdmFSdW50aW1lTWFu
aWZlc3RTZXJ2aWNlLmNsYXNzUEsBAhQAFAAICAgAp3X0XEydrnCAAwAAFwcAADYAAAAAAAAAAAAA
AAAAK3kAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9qYXZhL0phdmFSdW50aW1lUGFja2FnZS5j
bGFzc1BLAQIUABQACAgIAKd19FyoJq69MBoAAEg5AAA2AAAAAAAAAAAAAAAAAA99AABuZXQvbGl0
ZWxhdW5jaGVyL2JhY2tlbmQvamF2YS9KYXZhUnVudGltZVNlcnZpY2UuY2xhc3NQSwECCgAKAAAI
AACndfRcAAAAAAAAAAAAAAAAIgAAAAAAAAAAAAAAAACjlwAAbmV0L2xpdGVsYXVuY2hlci9iYWNr
ZW5kL3BsYXRmb3JtL1BLAQIUABQACAgIAKd19FyjY4RDLAQAALYIAAA1AAAAAAAAAAAAAAAAAOOX
AABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvcGxhdGZvcm0vTGF1bmNoZXJQYXRocy5jbGFzc1BL
AQIUABQACAgIAKd19FxMZ9MwrQQAAO8JAAA3AAAAAAAAAAAAAAAAAHKcAABuZXQvbGl0ZWxhdW5j
aGVyL2JhY2tlbmQvcGxhdGZvcm0vT3BlcmF0aW5nU3lzdGVtLmNsYXNzUEsBAhQAFAAICAgAp3X0
XMjdOUF3CQAAFRMAAC8AAAAAAAAAAAAAAAAAhKEAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9w
bGF0Zm9ybS9PU1V0aWxzLmNsYXNzUEsBAgoACgAACAAAp3X0XAAAAAAAAAAAAAAAABsAAAAAAAAA
AAAAAAAAWKsAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL1BLAQIUABQACAgIAKd19Fz4PCbC
lAIAAKQFAAA/AAAAAAAAAAAAAAAAAJGrAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290
c3RyYXAkQm9vdHN0cmFwU2NlbmUkU3RhZ2UuY2xhc3NQSwECFAAUAAgICACndfRcmNT2nf0IAACW
EwAAOQAAAAAAAAAAAAAAAACSrgAAbmV0L2xpdGVsYXVuY2hlci9ib290c3RyYXAvQm9vdHN0cmFw
JEJvb3RzdHJhcFNjZW5lLmNsYXNzUEsBAhQAFAAICAgAp3X0XLzV0QOwBAAA3goAADYAAAAAAAAA
AAAAAAAA9rcAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL0Jvb3RzdHJhcCRCb290c3RyYXBV
aS5jbGFzc1BLAQIUABQACAgIAKd19FxLzbywigUAALMMAAAqAAAAAAAAAAAAAAAAAAq9AABuZXQv
bGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290c3RyYXAuY2xhc3NQSwECFAAUAAgICACndfRctego
zFUNAAAFHgAAMQAAAAAAAAAAAAAAAADswgAAbmV0L2xpdGVsYXVuY2hlci9ib290c3RyYXAvQm9v
dHN0cmFwQmFja2VuZC5jbGFzc1BLAQIUABQACAgIAKd19FxZbCslOAEAAD4CAAAzAAAAAAAAAAAA
AAAAAKDQAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290c3RyYXBFeGNlcHRpb24uY2xh
c3NQSwECFAAUAAgICACndfRciVhWDmENAAD8HwAANwAAAAAAAAAAAAAAAAA50gAAbmV0L2xpdGVs
YXVuY2hlci9ib290c3RyYXAvTGF1bmNoZXJJbnN0YWxsU2VydmljZS5jbGFzc1BLAQIUABQACAgI
AKd19FzX9KwfdQwAAA8bAAAxAAAAAAAAAAAAAAAAAP/fAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3Rz
dHJhcC9MYXVuY2hlck1hbmlmZXN0LmNsYXNzUEsBAhQAFAAICAgAp3X0XAbovRGADgAA9h8AADgA
AAAAAAAAAAAAAAAA0+wAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL0xhdW5jaGVyTWFuaWZl
c3RTZXJ2aWNlLmNsYXNzUEsBAhQAFAAICAgApnX0XKclgWK+AgAAmwYAAC0AAAAAAAAAAAAAAAAA
ufsAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL01hbmlmZXN0TG9hZC5jbGFzc1BLAQIKAAoA
AAgAAKd19FwAAAAAAAAAAAAAAAAUAAAAAAAAAAAAAAAAANL+AABuZXQvbGl0ZWxhdW5jaGVyL3Vp
L1BLAQIUABQACAgIAKd19FzxV09bwQYAAHkMAAAjAAAAAAAAAAAAAAAAAAT/AABuZXQvbGl0ZWxh
dW5jaGVyL3VpL0FwcFdpbmRvdy5jbGFzc1BLAQIUABQACAgIAKZ19FyeGC+ZmAQAALsIAAAhAAAA
AAAAAAAAAAAAABYGAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL0hvdHNwb3QuY2xhc3NQSwECFAAUAAgI
CACmdfRcMNaL4FYDAABZBgAAJQAAAAAAAAAAAAAAAAD9CgEAbmV0L2xpdGVsYXVuY2hlci91aS9N
b3VzZUN1cnNvci5jbGFzc1BLAQIUABQACAgIAKZ19Fz6ENkzXAQAAJYJAAAkAAAAAAAAAAAAAAAA
AKYOAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL01vdXNlU3RhdGUuY2xhc3NQSwECFAAUAAgICACndfRc
nmvem24CAAAhBAAAIQAAAAAAAAAAAAAAAABUEwEAbmV0L2xpdGVsYXVuY2hlci91aS9QYWxldHRl
LmNsYXNzUEsBAhQAFAAICAgAp3X0XHmvYqULAQAAzwEAAC4AAAAAAAAAAAAAAAAAERYBAG5ldC9s
aXRlbGF1bmNoZXIvdWkvUGl4ZWxCdXR0b24kUmVuZGVyZXIuY2xhc3NQSwECFAAUAAgICACndfRc
2N5mRq4CAAA8BQAAKwAAAAAAAAAAAAAAAAB4FwEAbmV0L2xpdGVsYXVuY2hlci91aS9QaXhlbEJ1
dHRvbiRTdGF0ZS5jbGFzc1BLAQIUABQACAgIAKd19FxTPp9oXwYAAMAMAAAlAAAAAAAAAAAAAAAA
AH8aAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL1BpeGVsQnV0dG9uLmNsYXNzUEsBAhQAFAAICAgApnX0
XK4rHaECBAAAQQgAADIAAAAAAAAAAAAAAAAAMSEBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxD
YW52YXMkSW5wdXRIYW5kbGVyLmNsYXNzUEsBAhQAFAAICAgApnX0XE1+D/WqCAAALBEAACUAAAAA
AAAAAAAAAAAAkyUBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxDYW52YXMuY2xhc3NQSwECFAAU
AAgICACmdfRc/8QmNpkPAAB9HgAAJwAAAAAAAAAAAAAAAACQLgEAbmV0L2xpdGVsYXVuY2hlci91
aS9QaXhlbEdyYXBoaWNzLmNsYXNzUEsBAhQAFAAICAgAp3X0XLZJ+qdzCQAAQRYAACYAAAAAAAAA
AAAAAAAAfj4BAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxQYWludGVyLmNsYXNzUEsBAhQAFAAI
CAgApnX0XAKdmqCOAgAAcgUAACYAAAAAAAAAAAAAAAAARUgBAG5ldC9saXRlbGF1bmNoZXIvdWkv
UGl4ZWxTdXJmYWNlLmNsYXNzUEsBAhQAFAAICAgAp3X0XJMvP1SRAgAAEAUAAC0AAAAAAAAAAAAA
AAAAJ0sBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxUZXh0JEFsaWdubWVudC5jbGFzc1BLAQIU
ABQACAgIAKd19FwWU9IGJAgAAOEPAAAjAAAAAAAAAAAAAAAAABNOAQBuZXQvbGl0ZWxhdW5jaGVy
L3VpL1BpeGVsVGV4dC5jbGFzc1BLAQIUABQACAgIAKZ19Fxql11MdAIAADUEAAAlAAAAAAAAAAAA
AAAAAIhWAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL1Byb2dyZXNzQmFyLmNsYXNzUEsBAhQAFAAICAgA
pnX0XBWe8NXAAAAA8QAAACYAAAAAAAAAAAAAAAAAT1kBAG5ldC9saXRlbGF1bmNoZXIvdWkvVGFz
a1Byb2dyZXNzLmNsYXNzUEsBAgoACgAACAAAp3X0XAAAAAAAAAAAAAAAABkAAAAAAAAAAAAAAAAA
Y1oBAG5ldC9saXRlbGF1bmNoZXIvdWkvdGV4dC9QSwECFAAUAAgICACndfRc0VlGJFcDAADVBQAA
KgAAAAAAAAAAAAAAAACaWgEAbmV0L2xpdGVsYXVuY2hlci91aS90ZXh0L0dseXBoTGF5b3V0LmNs
YXNzUEsBAhQAFAAICAgApnX0XPxSG4mRAwAAvAgAAC0AAAAAAAAAAAAAAAAASV4BAG5ldC9saXRl
bGF1bmNoZXIvdWkvdGV4dC9UZXh0Rm9udCRHbHlwaC5jbGFzc1BLAQIUABQACAgIAKZ19Fxq6CYz
ij0AAAF+AAAnAAAAAAAAAAAAAAAAADViAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL3RleHQvVGV4dEZv
bnQuY2xhc3NQSwECFAAUAAgICACmdfRcMny079MHAAC6DgAALQAAAAAAAAAAAAAAAAAUoAEAbmV0
L2xpdGVsYXVuY2hlci91aS90ZXh0L1RleHRSYXN0ZXJpemVyLmNsYXNzUEsBAhQAFAAICAgAp3X0
XEVxQRVrBwAAehAAACYAAAAAAAAAAAAAAAAAQqgBAG5ldC9saXRlbGF1bmNoZXIvdWkvV2luZG93
Q2hyb21lLmNsYXNzUEsFBgAAAAA+AD4A7RQAAAGwAQAAAA==
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
