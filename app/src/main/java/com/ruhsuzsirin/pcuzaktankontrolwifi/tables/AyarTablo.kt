package com.ruhsuzsirin.pcuzaktankontrolwifi.tables

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
@RealmClass
open class AyarTablo:RealmObject() {
    @PrimaryKey
    var id:Int = 0
    lateinit var ip:String
    lateinit var port:String
}