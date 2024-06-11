package utils
import com.github.t3hnar.bcrypt.*

import scala.util.Try

def hashPassword(password: String): Try[String] = {
  password.bcryptSafeBounded
}

def checkPassword(password: String, passwordHash: String): Try[Boolean] = {
  password.isBcryptedSafeBounded(passwordHash)
}
