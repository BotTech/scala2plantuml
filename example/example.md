@startuml
class A extends B {
  + {method} <init>
  + {method} b
}
A o-- C
interface B {
  + {abstract} {method} b
}
B o-- C
class C {
  + {method} <init>
  + {field} value
}
C o-- A
class Main {
  + {static} {field} a
}
Main o-- A
@enduml
