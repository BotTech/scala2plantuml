import sbt.Keys.skip
import sbt.{Def, InputKey, InputTask, Task, TaskKey}

object ConditionalKeys {

  def settingDefaultIfSetting[A, B](
      setting: Def.Initialize[A],
      condition: Def.Initialize[B],
      default: => A
    )(
      p: B => Boolean
    ): Def.Initialize[A] = Def.setting {
    if (p(condition.value)) {
      default
    } else {
      setting.value
    }
  }

  def taskDefaultIfSkipped[A](task: TaskKey[A], default: => A): Def.Initialize[Task[A]] =
    taskDefaultIfTask(task, task / skip, default)(identity)

  def taskDefaultIfSetting[A, B](
      task: Def.Initialize[Task[A]],
      condition: Def.Initialize[B],
      default: => A
    )(
      p: B => Boolean
    ): Def.Initialize[Task[A]] = Def.taskIf {
    if (p(condition.value)) {
      default
    } else {
      task.value
    }
  }

  def taskDefaultIfTask[A, B](
      task: Def.Initialize[Task[A]],
      condition: Def.Initialize[Task[B]],
      default: => A
    )(
      p: B => Boolean
    ): Def.Initialize[Task[A]] = Def.taskIf {
    if (p(condition.value)) {
      default
    } else {
      task.value
    }
  }

  def inputDefaultIfSkipped[A](input: InputKey[A], default: => A): Def.Initialize[InputTask[A]] =
    inputDefaultIfTask(input, input / skip, default)(identity)

  def inputDefaultIfTask[A, B](
      input: InputKey[A],
      condition: Def.Initialize[Task[B]],
      default: => A
    )(
      p: B => Boolean
    ): Def.Initialize[InputTask[A]] = Def.inputTaskDyn {
    val task = input.parsed
    Def.taskIf {
      if (p(condition.value)) {
        default
      } else {
        task.value
      }
    }
  }
}
