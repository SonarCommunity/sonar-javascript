
class my_class { // Noncompliant {{Rename class "my_class" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}
//    ^^^^^^^^

}

class MyClass {

}

var x = class y { // Compliant, rule doesn't check class expressions
}
