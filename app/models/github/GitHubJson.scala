package models.github

import java.util.Date
import play.api.libs.json._
import play.api.libs.json.Reads.list
import play.api.libs.functional.syntax._

object GitHubJson {

  val readGitHubUser: Reads[GitHubUser] =
    (
      (__ \ 'login).read[String] and
      (__ \ 'html_url).read[String] and
      (__ \ 'hireable).readNullable[Boolean] and
      (__ \ 'followers).read[Long] and
      (__ \ 'blog).readNullable[String] and
      (__ \ 'bio).readNullable[String] and
      (__ \ 'email).readNullable[String] and
      (__ \ 'name).readNullable[String] and
      (__ \ 'company).readNullable[String] and
      (__ \ 'avatar_url).readNullable[String] and
      (__ \ 'gravatar_id).readNullable[String] and
      (__ \ 'location).readNullable[String]
    )((login, url, hireable, followers, blog, bio, email, name, company, avatar, gravatar, location) =>
      GitHubUser(login, url, hireable.filter(_ == true).isDefined, followers, blog, bio, email, name, company, avatar, gravatar, location))

  val readSearchedUser: Reads[GitHubSearchedUser] =
    (
      (__ \ 'login).read[String] and
      (__ \ 'fullname).readNullable[String] and
      (__ \ 'language).readNullable[String] and
      (__ \ 'followers).readNullable[Int] and
      (__ \ 'location).readNullable[String] and
      (__ \ 'public_repo_count).readNullable[Int]
    )(GitHubSearchedUser)

  val readSearchedUsers: Reads[List[GitHubSearchedUser]] = list(readSearchedUser)

  val readOrganization: Reads[GitHubOrg] =
    (
      (__ \ 'login).read[String] and
      (__ \ 'repos_url).read[String] and
      (__ \ 'avatar_url).readNullable[String] and
      (__ \ 'url).read[String]
    )((login, reposUrl, avatarUrl, url) => GitHubOrg(login, reposUrl, avatarUrl, url))

  val readOrganizations: Reads[List[GitHubOrg]] = list(readOrganization)

  val readRepository: Reads[GitHubRepository] = {
    (
      (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'language).readNullable[String] and
      (__ \ 'html_url).read[String] and
      (__ \ 'owner \ 'login).read[String] and
      (__ \ 'forks_count).read[Int] and
      (__ \ 'watchers_count).read[Int] and
      (__ \ 'fork).read[Boolean] and
      (__ \ 'updated_at).read[Date]
    ) ((name, desc, lang, htmlUrl, owner, forks, watchers, fork, updatedAt) =>
      GitHubRepository(name, desc, lang, htmlUrl, owner, forks, watchers, fork, updatedAt)
    )
  }

  val readRepositories: Reads[List[GitHubRepository]] = list(readRepository)

}
