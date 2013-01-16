# Конфигурационный файл jNode 0.3.4
# Комментарии начинаются с '#'
# Название узла
info.stationname = Sample Node
# Где находится
info.location = The City Of Country
# Кто сисоп
info.sysop = Rodriguez bender
# NDL - флаги нодлиста
info.ndl = 115200,TCP,BINKP
# FTN-адрес узла
info.address = 2:256/128@fidonet
# Данные для коннекта к базе
jdbc.url = jdbc:postgresql://localhost/database
jdbc.user = username
jdbc.pass = password
# Что слушать для входящих соединений
binkp.bind = 0.0.0.0
# Какой порт слушать
binkp.port = 24554
# Папка для приёма файлов
binkp.inbound = /var/spool/jnode/inbound
# Закомментируйте если хотите выключить сервер
binkp.server = 1
# Закомментируйте если хотите выключить клиент
binkp.client = 1
# Задержка перед первым поллом после стартапа
poll.delay = 600
# Периоды ожидания между поллами ( после первого полла )
poll.period = 600
# Логлевел, от 5 до 1
log.level = 4
# Путь к нодлисту ( только читать )
nodelist.path = /var/spool/jnode/NODELIST
# Путь к индексу нодлиста ( читать и писать )
nodelist.index = /var/spool/jnode/NODELIST.idx
# Закоментируйте чтоб отключить обработку файлэх
fileecho.enable = 1
# Путь к фэхам
fileecho.path = /var/spool/jnode/files
# Раскомментируйте, чтобы включить постинг статистики
#stat.enable = 1
# Эхоконференция, в которую пишут постеры статистики
stat.area = jnode.local.stat
# Раскомментируйте, чтобы включить запуск пользовательских скриптов по расписанию
#jscript.enable = 1
