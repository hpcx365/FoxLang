import pers.hpcx.foxlang.cli.FoxCli
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(FoxCli.run(args))
}
